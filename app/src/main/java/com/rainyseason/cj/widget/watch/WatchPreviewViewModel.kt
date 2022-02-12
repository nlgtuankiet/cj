package com.rainyseason.cj.widget.watch

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.os.Parcelable
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.getWidgetId
import com.rainyseason.cj.common.isOnboardDone
import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.Watchlist
import com.rainyseason.cj.common.model.WatchlistCollection
import com.rainyseason.cj.common.setOnboardDone
import com.rainyseason.cj.common.update
import com.rainyseason.cj.common.usecase.GetWatchDisplayEntry
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.TimeUnit

data class WatchPreviewState(
    val savedDisplayData: Async<WatchDisplayData> = Uninitialized,
    val savedConfig: Async<WatchConfig> = Uninitialized,
    val userSetting: Async<UserSetting> = Uninitialized,
    val watchListCollection: Async<WatchlistCollection> = Uninitialized,
    val watchDisplayData: Map<WatchDisplayEntryLoadParam, Async<WatchDisplayEntryContent>> =
        emptyMap(),
    val previewScale: Double? = null,
    val scalePreview: Boolean = true,
    val showAdvanceSetting: Boolean = false,
    val saved: Boolean = false,
) : MavericksState {
    val config: WatchConfig?
        get() = savedConfig.invoke()

    val displayData: WatchDisplayData?
        get() = savedDisplayData.invoke()
}

@Parcelize
data class WatchPreviewArgs(
    val widgetId: Int,
    val debugLayout: String?,
) : Parcelable

class WatchPreviewViewModel @AssistedInject constructor(
    @Assisted val initState: WatchPreviewState,
    @Assisted val args: WatchPreviewArgs,
    private val watchWidgetRepository: WatchWidgetRepository,
    private val watchListRepository: WatchListRepository,
    private val userSettingRepository: UserSettingRepository,
    private val appWidgetManager: AppWidgetManager,
    private val keyValueStore: KeyValueStore,
    private val getWatchDisplayEntry: GetWatchDisplayEntry,
    private val watchWidgetHandler: WatchWidgetHandler,
    private val watchWidgetRender: WatchWidgetRender,
    private val tracker: Tracker,
) : MavericksViewModel<WatchPreviewState>(initState) {

    private val loadWatchEntryDataJob: MutableMap<WatchDisplayEntryLoadParam, Job> =
        Collections.synchronizedMap(mutableMapOf())

    private var watchlistCollection: Job? = null
    private var loadWatchListEntriesJob: Job? = null

    private val _onBoardFeature = Channel<WatchOnboardFeature>()
    val onBoardFeature = _onBoardFeature.receiveAsFlow()

    init {
        loadDisplayData()
        loadWatchlistEntry()
        maybeSaveDisplayData()
        viewModelScope.launch {
            saveInitialConfig()
        }
        checkNextOnBoard()
    }

    private fun maybeSaveDisplayData() {
        viewModelScope.launch {
            stateFlow.mapNotNull { state ->
                val config = state.config ?: return@mapNotNull null
                val watchlist = state.watchListCollection
                    .invoke()?.list?.firstOrNull { it.id == Watchlist.DEFAULT_ID }
                    ?.coins?.take(config.layout.entryLimit)
                    ?: return@mapNotNull null
                val entries = watchlist.map { coin ->
                    val params = WatchDisplayEntryLoadParam(
                        coin = coin,
                        currency = config.currency,
                        changeInterval = config.interval,
                    )
                    val content = state.watchDisplayData[params]?.invoke()
                    WatchDisplayEntry(
                        coinId = coin.id,
                        backend = coin.backend,
                        network = coin.network,
                        dex = coin.dex,
                        content = content
                    )
                }
                WatchDisplayData(
                    entries = entries
                )
            }
                .distinctUntilChanged()
                .collect { data ->
                    watchWidgetRepository.setDisplayData(args.widgetId, data)
                }
        }
    }

    private fun loadWatchlistEntry() {
        watchlistCollection?.cancel()
        watchlistCollection = watchListRepository.getWatchlistCollectionFlow()
            .execute { copy(watchListCollection = it) }

        loadWatchListEntriesJob?.cancel()
        loadWatchListEntriesJob = onEach(
            WatchPreviewState::watchListCollection,
            WatchPreviewState::config,
        ) { watchListCollection, config ->
            config ?: return@onEach
            val collection = watchListCollection.invoke() ?: return@onEach
            val watchlist = collection.list.firstOrNull { it.id == Watchlist.DEFAULT_ID }
                ?: return@onEach
            watchlist.coins
                .take(config.layout.entryLimit)
                .forEach { coin ->
                    val param = WatchDisplayEntryLoadParam(
                        coin = coin,
                        currency = config.currency,
                        changeInterval = config.interval,
                    )
                    maybeLoadWatchEntry(param)
                }
        }
    }

    private fun maybeLoadWatchEntry(param: WatchDisplayEntryLoadParam) {
        withState { state ->
            val currentAsync = state.watchDisplayData[param]
            if (currentAsync == null || currentAsync is Fail) {
                Timber.d("load $param")
                loadWatchEntryDataJob[param]?.cancel()
                loadWatchEntryDataJob[param] = suspend {
                    getWatchDisplayEntry.invoke(param)
                }.execute {
                    copy(watchDisplayData = watchDisplayData.update { put(param, it) })
                }
            }
        }
    }

    private fun checkNextOnBoard() {
        viewModelScope.launch {
            val features = WatchOnboardFeature.values()
            var currentFeatureIndex = 0
            while (currentFeatureIndex <= features.lastIndex) {
                val feature = features[currentFeatureIndex]
                val done = keyValueStore.isOnboardDone(feature.featureName)
                if (!done) {
                    _onBoardFeature.send(feature)
                    return@launch
                }
                currentFeatureIndex++
            }
        }
    }

    private fun getLayout(): WatchWidgetLayout {
        if (args.debugLayout != null) {
            return WatchWidgetLayout.values().first { it.id == args.debugLayout }
        }
        val providerName = appWidgetManager.getAppWidgetInfo(args.widgetId)?.provider?.className
            ?: WatchWidgetLayout.Watch4x2.providerName
        return WatchWidgetLayout.fromProviderName(providerName)
    }

    fun switchScalePreview() {
        setState { copy(scalePreview = !scalePreview) }
    }

    private suspend fun saveInitialConfig() {
        val userSetting = userSettingRepository.getUserSetting()
        val layout = getLayout()
        val previousConfig = watchWidgetRepository.getConfig(args.widgetId)
        if (previousConfig != null) {
            setState { copy(saved = true) }
        }
        val config = previousConfig ?: WatchConfig(
            widgetId = args.widgetId,
            interval = TimeInterval.I_24H,
            currency = userSetting.currencyCode,
            refreshInterval = userSetting.refreshInterval,
            refreshIntervalUnit = userSetting.refreshIntervalUnit,
            numberOfChangePercentDecimal = userSetting.numberOfChangePercentDecimal ?: 1,
            layout = layout
        )

        watchWidgetRepository.setConfig(args.widgetId, config.ensureValid())
        setState { copy(previewScale = layout.previewScale) }
        loadConfig()
    }

    private fun loadDisplayData() {
        watchWidgetRepository.getDisplayDataStream(args.widgetId)
            .execute { copy(savedDisplayData = it) }
    }

    private var loadConfigJob: Job? = null
    private fun loadConfig() {
        loadConfigJob?.cancel()
        loadConfigJob = watchWidgetRepository.getConfigStream(args.widgetId)
            .execute { copy(savedConfig = it) }
    }

    @SuppressLint("MissingSuperCall")
    override fun onCleared() {
        withState { state ->
            viewModelScope.launch {
                if (state.saved) {
                    Timber.d("Widget saved")
                    val config = state.config
                    val data = state.displayData
                    if (config != null && data != null) {
                        val params = WatchWidgetRenderParams(
                            config = config,
                            data = data,
                            showLoading = false,
                            isPreview = false,
                        )
                        watchWidgetRender.render(args.widgetId, params)
                    }
                    tracker.logKeyParamsEvent(
                        key = "widget_save",
                        params = config?.getTrackingParams().orEmpty(),
                    )
                    watchWidgetHandler.enqueueRefreshWidget(
                        widgetId = args.widgetId,
                        config = config
                    )
                } else {
                    watchWidgetHandler.onWidgetDelete(args.widgetId)
                    Timber.d("Cleared all data")
                }
                viewModelScope.cancel()
            }
        }
    }

    fun setRefreshInternal(interval: Long, unit: TimeUnit) {
        updateConfig {
            copy(refreshInterval = interval, refreshIntervalUnit = unit)
        }
    }

    private fun updateConfig(block: WatchConfig.() -> WatchConfig) {
        withState { state ->
            val config = state.savedConfig.invoke()
            if (config != null) {
                val newConfig = block.invoke(config).ensureValid()
                maybeSaveConfig(newConfig)
            }
        }
    }

    private fun maybeSaveConfig(config: WatchConfig) {
        viewModelScope.launch {
            watchWidgetRepository.setConfig(args.widgetId, config)
        }
    }

    fun setCurrency(value: String) {
        updateConfig { copy(currency = value) }
    }

    fun setTheme(value: Theme) {
        updateConfig { copy(theme = value) }
    }

    fun setAdjustment(value: Int) {
        updateConfig { copy(sizeAdjustment = value) }
    }

    fun setBackgroundTransparency(value: Int) {
        updateConfig { copy(backgroundTransparency = value) }
    }

    fun showAdvanced() {
        setState { copy(showAdvanceSetting = true) }
    }

    fun setNumberOfDecimal(value: Int) {
        updateConfig { copy(numberOfAmountDecimal = value) }
    }

    fun switchRoundToMillion() {
        updateConfig { copy(roundToMillion = !roundToMillion) }
    }

    fun switchShowThousandsSeparator() {
        updateConfig { copy(showThousandsSeparator = !showThousandsSeparator) }
    }

    fun switchHideDecimalOnLargePrice() {
        updateConfig { copy(hideDecimalOnLargePrice = !hideDecimalOnLargePrice) }
    }

    fun setPriceChangeInterval(selectedInterval: TimeInterval) {
        updateConfig { copy(interval = selectedInterval) }
    }

    fun setNumberOfChangePercentDecimal(value: Int) {
        updateConfig { copy(numberOfChangePercentDecimal = value) }
    }

    fun switchShowBatteryWarning() {
        updateConfig { copy(showBatteryWarning = !showBatteryWarning) }
    }

    fun save() {
        setState { copy(saved = true) }
    }

    fun setClickAction(action: WatchClickAction) {
        updateConfig { copy(clickAction = action) }
    }

    fun switchShowCurrency() {
        updateConfig { copy(showCurrencySymbol = !showCurrencySymbol) }
    }

    fun toggleFullSize() {
        updateConfig { copy(fullSize = !fullSize) }
    }

    fun onOnBoardDone(feature: WatchOnboardFeature) {
        viewModelScope.launch {
            keyValueStore.setOnboardDone(feature.featureName)
            checkNextOnBoard()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initState: WatchPreviewState,
            args: WatchPreviewArgs,
        ): WatchPreviewViewModel
    }

    companion object : MavericksViewModelFactory<WatchPreviewViewModel, WatchPreviewState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: WatchPreviewState
        ): WatchPreviewViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<WatchPreviewFragment>()
            val factory = fragment.viewModelFactory
            val extra = fragment.requireActivity().intent.extras
            val widgetId = extra?.getWidgetId()
                ?: throw IllegalArgumentException("missing widget id")
            val debugLayout = extra.getString("debug_layout")

            return factory.create(state, WatchPreviewArgs(widgetId, debugLayout))
        }
    }
}
