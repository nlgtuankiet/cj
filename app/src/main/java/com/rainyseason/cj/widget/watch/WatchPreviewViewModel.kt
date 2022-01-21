package com.rainyseason.cj.widget.watch

import android.appwidget.AppWidgetManager
import android.os.Parcelable
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.getWidgetId
import com.rainyseason.cj.common.isOnboardDone
import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.asDayString
import com.rainyseason.cj.common.setOnboardDone
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.MarketChartResponse
import com.rainyseason.cj.data.database.kv.KeyValueStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.concurrent.TimeUnit

data class WatchPreviewState(
    val savedDisplayData: Async<WatchDisplayData> = Uninitialized,
    val savedConfig: Async<WatchConfig> = Uninitialized,
    val watchlist: Async<List<String>> = Uninitialized,
    val userSetting: Async<UserSetting> = Uninitialized,
    val coinDetail: Map<String, Async<CoinDetailResponse>> = emptyMap(),
    val coinMarket: Map<String, Async<MarketChartResponse>> = emptyMap(),
    val previewScale: Double? = null,
    val scalePreview: Boolean = true,
    val showAdvanceSetting: Boolean = false,
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
    private val coinGeckoService: CoinGeckoService,
    private val appWidgetManager: AppWidgetManager,
    private val keyValueStore: KeyValueStore,
) : MavericksViewModel<WatchPreviewState>(initState) {
    private var saved = false

    private var loadCoinDetailJob: Job? = null
    private val loadCoinDetailJobs = mutableMapOf<String, Job>()
    private var loadEachEntryDataJob: Job? = null
    private val loadCoinMarketJobs = mutableMapOf<String, Job>()

    private val _onBoardFeature = Channel<WatchOnboardFeature>()
    val onBoardFeature = _onBoardFeature.receiveAsFlow()

    init {
        loadDisplayData()
        loadWatchList()
        loadUserSetting()

        onEach { state ->
            maybeSaveDisplayData(state)
        }

        reload()
        checkNextOnBoard()
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

    private fun reload() {
        viewModelScope.launch {
            saveInitialConfig()
        }
        loadEachEntryData()
        loadCoinDetail()
    }

    private fun loadCoinDetail() {
        loadCoinDetailJob?.cancel()
        loadCoinDetailJob = onAsync(
            WatchPreviewState::watchlist
        ) { watchlist ->
            loadCoinDetailJobs.values.forEach { it.cancel() }
            setState { copy(coinDetail = emptyMap()) }
            // TODO replace with widget type
            watchlist.forEach { coinId ->
                loadCoinDetailJobs[coinId] = suspend {
                    coinGeckoService.getCoinDetail(coinId)
                }.execute { copy(coinDetail = coinDetail.update { put(coinId, it) }) }
            }
        }
    }

    private fun loadUserSetting() {
        userSettingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }
    }

    private fun loadEachEntryData() {
        loadEachEntryDataJob?.cancel()
        loadEachEntryDataJob = viewModelScope.launch {
            stateFlow
                .mapNotNull { state ->
                    val userSetting = state.userSetting.invoke()
                    val watchlist = state.watchlist.invoke()
                    val config = state.config
                    if (userSetting == null || watchlist == null || config == null) {
                        null
                    } else {
                        Triple(userSetting.currencyCode, config.interval, watchlist)
                    }
                }
                .distinctUntilChanged()
                .collect { (currencyCode, interval, watchlist) ->
                    loadCoinMarket(currencyCode, interval, watchlist)
                }
        }
    }

    private fun loadCoinMarket(
        currencyCode: String,
        interval: TimeInterval,
        watchlist: List<String>
    ) {
        loadCoinMarketJobs.values.forEach { it.cancel() }
        loadCoinMarketJobs.clear()
        setState { copy(coinMarket = emptyMap()) }
        watchlist.forEach { coinId ->
            suspend {
                coinGeckoService.getMarketChart(
                    coinId,
                    currencyCode,
                    interval.asDayString()!!
                )
            }.execute {
                copy(coinMarket = coinMarket.update { put(coinId, it) })
            }
        }
    }

    // TODO only save when something changed
    private suspend fun maybeSaveDisplayData(state: WatchPreviewState) {
        val watchlist = state.watchlist.invoke() ?: return
        val currencyCode = state.userSetting.invoke()?.currencyCode ?: return
        val entries = watchlist.map { coinId ->
            val coinDetail = state.coinDetail[coinId]?.invoke()
                ?: return@map WatchDisplayEntry(coinId, null)
            val coinMarket = state.coinMarket[coinId]?.invoke()
            val priceChart = coinMarket?.prices?.takeIf { it.size >= 2 }
            WatchDisplayEntry(
                coinId = coinId,
                content = WatchDisplayEntryContent(
                    symbol = coinDetail.symbol,
                    name = coinDetail.name,
                    graph = priceChart,
                    price = coinDetail.marketData.currentPrice[currencyCode] ?: 0.0,
                    changePercent = priceChart?.changePercent()?.let { it * 100 }
                )
            )
        }
        val data = WatchDisplayData(entries)
        watchWidgetRepository.setDisplayData(args.widgetId, data)
    }

    private fun getLayout(): WatchWidgetLayout {
        if (args.debugLayout != null) {
            return WatchWidgetLayout.values().first { it.id == args.debugLayout }
        }
        val providerName = appWidgetManager.getAppWidgetInfo(args.widgetId)?.provider?.className
            ?: WatchWidgetLayout.Watch4x2.providerName
        return WatchWidgetLayout.fromProviderName(providerName)
    }

    @ExperimentalCoroutinesApi
    private fun loadWatchList() {
        stateFlow.mapNotNull {
            it.config?.fullSize
        }.flatMapLatest { fullSize ->
            watchListRepository.getWatchList()
                .map {
                    it.take(
                        if (fullSize) {
                            Int.MAX_VALUE
                        } else {
                            getLayout().entryLimit
                        }
                    )
                }
        }.execute { copy(watchlist = it) }
    }

    fun switchScalePreview() {
        setState { copy(scalePreview = !scalePreview) }
    }

    private suspend fun saveInitialConfig() {
        val userSetting = userSettingRepository.getUserSetting()
        val layout = getLayout()
        val previousConfig = watchWidgetRepository.getConfig(args.widgetId)
        val config = previousConfig ?: WatchConfig(
            widgetId = args.widgetId,
            interval = TimeInterval.I_24H,
            currency = userSetting.currencyCode,
            refreshInterval = userSetting.refreshInterval,
            refreshIntervalUnit = userSetting.refreshIntervalUnit,
            numberOfChangePercentDecimal = userSetting.numberOfChangePercentDecimal ?: 1,
            layout = layout
        )

        watchWidgetRepository.setConfig(args.widgetId, config)
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

    override fun onCleared() {
        if (!saved) {
            viewModelScope.launch(NonCancellable) {
                watchWidgetRepository.clearDisplayData(args.widgetId)
            }
        }
        super.onCleared()
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
                val newConfig = block.invoke(config)
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
        saved = true
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
