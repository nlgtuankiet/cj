package com.rainyseason.cj.ticker.preview

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.Context
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.isOnboardDone
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.setOnboardDone
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.rainyseason.cj.ticker.CoinTickerDisplayData.LoadParam
import com.rainyseason.cj.ticker.CoinTickerHandler
import com.rainyseason.cj.ticker.CoinTickerLayout
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.TickerWidgetFeature
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.ticker.usecase.CreateDefaultTickerWidgetConfig
import com.rainyseason.cj.ticker.usecase.GetDisplayData
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections
import java.util.UUID
import java.util.concurrent.TimeUnit

data class CoinTickerPreviewState(
    val savedDisplayData: Async<CoinTickerDisplayData> = Uninitialized,
    val savedConfig: Async<CoinTickerConfig> = Uninitialized,
    val showAdvanceSetting: Boolean = false,
    val displayDataCache: Map<LoadParam, Async<CoinTickerDisplayData>> = emptyMap(),
    val widgetSaved: Boolean = false,
) : MavericksState {
    val config: CoinTickerConfig?
        get() = savedConfig.invoke()

    val currentDisplayData: CoinTickerDisplayData?
        get() = displayDataCache[config?.asDataLoadParams()]?.invoke()

    val loadDataParams: LoadParam?
        get() = config?.asDataLoadParams()
}

class CoinTickerPreviewViewModel @AssistedInject constructor(
    @Assisted private val initState: CoinTickerPreviewState,
    @Assisted private val args: CoinTickerPreviewArgs,
    private val coinTickerRepository: CoinTickerRepository,
    private val getDisplayData: GetDisplayData,
    private val keyValueStore: KeyValueStore,
    private val tickerWidgetRenderer: TickerWidgetRenderer,
    private val coinTickerHandler: CoinTickerHandler,
    private val tracker: Tracker,
    private val appWidgetManager: AppWidgetManager,
    private val createDefaultTickerWidgetConfig: CreateDefaultTickerWidgetConfig,
    private val context: Context,
) : MavericksViewModel<CoinTickerPreviewState>(initState) {

    val id = UUID.randomUUID().toString()

    private val _onBoardFeature = Channel<TickerWidgetFeature>()
    val onBoardFeature = _onBoardFeature.receiveAsFlow()

    init {
        loadDisplayData()

        onEach(CoinTickerPreviewState::currentDisplayData) { currentDisplayData ->
            saveDisplayData(currentDisplayData)
        }

        reload()
        checkOnBoardFeature()
    }

    var onEachDisplayDataJob: Job? = null
    private fun checkOnBoardFeature() {
        onEachDisplayDataJob?.cancel()
        onEachDisplayDataJob = onEach(CoinTickerPreviewState::savedDisplayData) { displayData ->
            if (displayData.invoke() != null) {
                TickerWidgetFeature.values()
                    .filter {
                        // temporary disable onboard how to select coin id
                        it !in listOf(TickerWidgetFeature.CoinId)
                    }
                    .forEach { feature ->
                        val done = keyValueStore.isOnboardDone(feature.featureName)
                        if (!done && feature.predicate.invoke()) {
                            _onBoardFeature.send(feature)
                            onEachDisplayDataJob?.cancel()
                            return@forEach
                        }
                    }
            }
        }
    }

    fun onBoardFeatureDone(feature: TickerWidgetFeature) {
        viewModelScope.launch {
            keyValueStore.setOnboardDone(feature.featureName)
            checkOnBoardFeature()
        }
    }

    fun reload() {
        loadData()
        viewModelScope.launch {
            saveInitialConfig()
        }
    }

    private var onEachParamJob: Job? = null
    private val getDisplayDataJob = Collections
        .synchronizedMap(mutableMapOf<LoadParam, Job>())

    private fun loadData() {
        onEachParamJob?.cancel()
        onEachParamJob = onEach(CoinTickerPreviewState::loadDataParams) { loadDataParams ->
            if (loadDataParams == null) {
                return@onEach
            }
            withState { state ->
                val previousData = state.displayDataCache[loadDataParams]
                if (previousData is Loading || previousData is Success) {
                    return@withState
                }
                getDisplayDataJob[loadDataParams]?.cancel()
                val job = suspend {
                    getDisplayData(loadDataParams)
                }.execute {
                    copy(
                        displayDataCache = displayDataCache
                            .update { put(loadDataParams, it) }
                    )
                }
                getDisplayDataJob[loadDataParams] = job
            }
        }
    }

    private suspend fun saveInitialConfig() {
        val lastConfig = coinTickerRepository.getConfig(
            widgetId = args.widgetId,
            returnDefaultConfigIfMissing = false
        )
        if (args.callingComponent == null) {
            setState { copy(widgetSaved = true) }
        }
        val (coinId, backend) = if (lastConfig != null) {
            lastConfig.coinId to lastConfig.backend
        } else {
            "bitcoin" to Backend.CoinGecko
        }
        @Suppress("IfThenToElvis")
        val config = if (lastConfig == null) {
            createDefaultTickerWidgetConfig
                .invoke(widgetId = args.widgetId)
                .copy(coinId = coinId, backend = backend)
        } else {
            lastConfig.copy(
                coinId = coinId,
                backend = backend,
            )
        }.ensureValid()

        coinTickerRepository.setConfig(args.widgetId, config)

        loadConfig()
    }

    private fun updateConfig(block: CoinTickerConfig.() -> CoinTickerConfig) {
        withState { state ->
            val config = state.savedConfig.invoke()
            if (config != null) {
                val newConfig = block.invoke(config).ensureValid()
                maybeSaveConfig(newConfig)
            }
        }
    }

    fun setCurrency(value: String) {
        updateConfig { copy(currency = value) }
    }

    fun setClickAction(value: String) {
        updateConfig { copy(clickAction = value) }
    }

    fun setPriceChangeInterval(value: TimeInterval) {
        updateConfig { copy(changeInterval = value) }
    }

    fun setBackgroundTransparency(value: Int) {
        updateConfig { copy(backgroundTransparency = value) }
    }

    fun setAdjustment(value: Int) {
        updateConfig { copy(sizeAdjustment = value) }
    }

    fun setLayout(value: CoinTickerLayout) {
        updateConfig { copy(layout = value) }
    }

    fun setTheme(theme: Theme) {
        updateConfig {
            copy(theme = theme)
        }
    }

    fun setRefreshInternal(interval: Long, unit: TimeUnit) {
        updateConfig {
            copy(refreshInterval = interval, refreshIntervalUnit = unit)
        }
    }

    fun setNumberOfDecimal(value: Int) {
        updateConfig {
            copy(numberOfAmountDecimal = value)
        }
    }

    fun setNumberOfChangePercentDecimal(value: Int) {
        updateConfig {
            copy(numberOfChangePercentDecimal = value)
        }
    }

    private fun loadDisplayData() {
        coinTickerRepository.getDisplayDataStream(args.widgetId)
            .execute { copy(savedDisplayData = it) }
    }

    private fun maybeSaveConfig(config: CoinTickerConfig) {
        if (config.widgetId <= 0 || config.coinId.isBlank()) {
            return
        }
        viewModelScope.launch {
            coinTickerRepository.setConfig(args.widgetId, config)
        }
    }

    private var loadConfigJob: Job? = null
    private fun loadConfig() {
        loadConfigJob?.cancel()
        loadConfigJob = coinTickerRepository.getConfigStream(args.widgetId)
            .execute { copy(savedConfig = it) }
    }

    fun switchShowBatteryWarning() {
        updateConfig { copy(showBatteryWarning = !showBatteryWarning) }
    }

    fun switchShowCurrency() {
        updateConfig { copy(showCurrencySymbol = !showCurrencySymbol) }
    }

    fun switchHideDecimal() {
        updateConfig { copy(hideDecimalOnLargePrice = !hideDecimalOnLargePrice) }
    }

    fun switchRoundToMillion() {
        updateConfig { copy(roundToMillion = !roundToMillion) }
    }

    fun switchThousandsSeparator() {
        updateConfig { copy(showThousandsSeparator = !showThousandsSeparator) }
    }

    private fun saveDisplayData(data: CoinTickerDisplayData?) {
        if (data == null) {
            return
        }
        viewModelScope.launch {
            coinTickerRepository.setDisplayData(args.widgetId, data)
        }
    }

    fun save() {
        setState { copy(widgetSaved = true) }
    }

    @SuppressLint("MissingSuperCall")
    override fun onCleared() {
        withState { state ->
            viewModelScope.launch {
                if (state.widgetSaved) {
                    Timber.d("Widget saved")
                    val config = state.config
                    val data = state.currentDisplayData
                    if (config != null && data != null) {
                        val param = CoinTickerRenderParams(
                            config = config,
                            data = data,
                            showLoading = false,
                        )
                        tickerWidgetRenderer.render(inputParams = param)
                    }
                    val componentPackage = args.callingComponent?.packageName

                    val componentParams = componentPackage?.let {
                        mapOf("calling_package" to it)
                    } ?: emptyMap<String, Any?>()
                    tracker.logKeyParamsEvent(
                        key = "widget_save",
                        params = config?.getTrackingParams().orEmpty() + componentParams,
                    )
                    coinTickerHandler.enqueueRefreshWidget(
                        widgetId = args.widgetId,
                        config = config
                    )
                    viewModelScope.cancel()
                    Timber.d("Scope canceled")
                } else {
                    coinTickerHandler.onDelete(args.widgetId)
                    Timber.d("Cleared all data")
                    viewModelScope.cancel()
                    Timber.d("Scope canceled")
                }
            }
        }
    }

    fun setAmount(value: String?) {
        updateConfig { copy(amount = value?.toDoubleOrNull() ?: 1.0) }
    }

    fun showAdvanced() {
        setState { copy(showAdvanceSetting = true) }
    }

    fun setCoin(
        coin: Coin,
    ) {
        updateConfig {
            copy(coinId = coin.id, backend = coin.backend, network = coin.network, dex = coin.dex)
        }
    }

    fun toggleFullSize() {
        updateConfig {
            copy(fullSize = !fullSize)
        }
    }

    fun toggleNotification() {
        updateConfig {
            copy(showNotification = !showNotification)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initState: CoinTickerPreviewState,
            args: CoinTickerPreviewArgs,
        ): CoinTickerPreviewViewModel
    }

    companion object :
        MavericksViewModelFactory<CoinTickerPreviewViewModel, CoinTickerPreviewState> {
        override fun initialState(viewModelContext: ViewModelContext): CoinTickerPreviewState {
            return CoinTickerPreviewState()
        }

        override fun create(
            viewModelContext: ViewModelContext,
            state: CoinTickerPreviewState,
        ): CoinTickerPreviewViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<CoinTickerPreviewFragment>()
            return fragment.viewModelFactory.create(state, fragment.args)
        }
    }
}
