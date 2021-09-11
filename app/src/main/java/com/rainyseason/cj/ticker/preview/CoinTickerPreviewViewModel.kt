package com.rainyseason.cj.ticker.preview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.MarketChartResponse
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.ticker.ChangeInterval
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class CoinTickerPreviewState(
    val savedDisplayData: Async<CoinTickerDisplayData> = Uninitialized,
    val savedConfig: Async<CoinTickerConfig> = Uninitialized,
    val userCurrency: Async<String> = Uninitialized,
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val marketChartResponse: Map<String, Async<MarketChartResponse>> = emptyMap(),
    val numberOfDecimal: Int? = null,
) : MavericksState {
    val config: CoinTickerConfig?
        get() = savedConfig.invoke()
}

class CoinTickerPreviewViewModel @AssistedInject constructor(
    @Assisted private val args: CoinTickerPreviewArgs,
    private val coinTickerRepository: CoinTickerRepository,
    private val userSettingRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
) : MavericksViewModel<CoinTickerPreviewState>(CoinTickerPreviewState()) {

    private val widgetId = args.widgetId
    private val loadGraphJobs = mutableMapOf<String, Job>()
    private var saved = false

    init {
        loadUserCurrency()
        loadConfig()
        loadDisplayData()
        loadCoinDetail()

        onEach { state ->
            maybeSaveDisplayData(state)
        }

        viewModelScope.launch {
            saveInitialConfig()
        }

        viewModelScope.launch {
            stateFlow.mapNotNull { state ->
                val savedConfig = state.savedConfig
                val userCurrency = state.userCurrency
                if (savedConfig !is Success) {
                    return@mapNotNull null
                }
                if (userCurrency !is Success) {
                    return@mapNotNull null
                }
                savedConfig.invoke().currency ?: userCurrency.invoke()
            }.distinctUntilChanged()
                .collect { currencyCode ->
                    ChangeInterval.ALL_PRICE_INTERVAL.forEach { interval ->
                        loadGraph(interval, currencyCode)
                    }
                }
        }
    }


    private suspend fun saveInitialConfig() {
        val lastConfig = coinTickerRepository.getConfig(widgetId = widgetId)
        if (lastConfig == null) {
            val config = CoinTickerConfig(
                widgetId = widgetId,
                coinId = args.coinId,
                layout = args.layout,
                numberOfPriceDecimal = 2,
                numberOfChangePercentDecimal = 1,
            )

            coinTickerRepository.setConfig(widgetId, config)
        }
    }

    private fun updateConfig(block: CoinTickerConfig.() -> CoinTickerConfig) {
        withState { state ->
            val config = state.savedConfig.invoke()
            if (config != null) {
                val newConfig = block.invoke(config)
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

    fun setPriceChangeInterval(value: String) {
        updateConfig { copy(changeInterval = value) }
    }

    fun setBottomContentType(type: String) {
        updateConfig { copy(bottomContentType = type) }
    }

    fun setLayout(value: String) {
        updateConfig { copy(layout = value) }
    }

    fun setExtraSize(extra: Int) {
        updateConfig {
            copy(extraSize = extra)
        }
    }

    fun setTheme(theme: String) {
        updateConfig {
            copy(theme = theme)
        }
    }

    fun setRefreshInternal(interval: Long, unit: TimeUnit) {
        updateConfig {
            copy(refreshInterval = interval, refreshIntervalUnit = unit)
        }
    }

    fun setNumberOfDecimal(value: String) {
        Timber.d("setNumberOfDecimal $value")
        val number = value.toIntOrNull()?.coerceAtLeast(0)
        updateConfig {
            copy(numberOfPriceDecimal = number)
        }
    }

    fun setNumberOfChangePercentDecimal(value: String) {
        Timber.d("setNumberOfChangePercentDecimal $value")
        val number = value.toIntOrNull()?.coerceAtLeast(0)
        updateConfig {
            copy(numberOfChangePercentDecimal = number)
        }
    }

    private fun loadDisplayData() {
        coinTickerRepository.getDisplayDataStream(widgetId)
            .execute { copy(savedDisplayData = it) }
    }

    private fun maybeSaveConfig(config: CoinTickerConfig) {
        if (config.widgetId <= 0 || config.coinId.isBlank()) {
            return
        }
        viewModelScope.launch {
            coinTickerRepository.setConfig(widgetId, config)
        }
    }

    private fun loadConfig() {
        coinTickerRepository.getConfigStream(widgetId)
            .execute { copy(savedConfig = it) }
    }

    private fun loadUserCurrency() {
        userSettingRepository.getCurrencyCodeFlow().execute {
            copy(userCurrency = it)
        }
    }

    private fun loadGraph(interval: String, currency: String) {
        val day = when (interval) {
            ChangeInterval._7D -> 7
            ChangeInterval._14D -> 14
            ChangeInterval._30D -> 30
            ChangeInterval._1Y -> 365
            else -> 1
        }
        loadGraphJobs[interval]?.cancel()
        loadGraphJobs[interval] = suspend {
            coinGeckoService.getMarketChart(id = args.coinId, vsCurrency = currency, day)
        }.execute {
            copy(marketChartResponse = marketChartResponse.update { set(interval, it) })
        }
    }

    private var loadCoinDetailJob: Job? = null
    private fun loadCoinDetail() {
        loadCoinDetailJob?.cancel()
        loadCoinDetailJob = suspend {
            coinGeckoService.getCoinDetail(args.coinId)
        }.execute {
            copy(coinDetailResponse = it)
        }
    }

    fun switchShowCurrency() {
        updateConfig { copy(showCurrencySymbol = !showCurrencySymbol) }
    }

    fun switchRoundToMillion() {
        updateConfig { copy(roundToMillion = !roundToMillion) }
    }

    fun switchThousandsSeparator() {
        updateConfig { copy(showThousandsSeparator = !showThousandsSeparator) }
    }

    private fun maybeSaveDisplayData(state: CoinTickerPreviewState) {
        val userCurrency = state.userCurrency.invoke() ?: return
        val coinDetail = state.coinDetailResponse.invoke() ?: return
        val config = state.config ?: return

        viewModelScope.launch {
            setWidgetData(
                config = config,
                coinDetail = coinDetail,
                marketChartResponse = state.marketChartResponse.mapValues { it.value.invoke() },
                userCurrency = userCurrency,
            )
        }
    }

    private suspend fun setWidgetData(
        config: CoinTickerConfig,
        coinDetail: CoinDetailResponse,
        marketChartResponse: Map<String, MarketChartResponse?>,
        userCurrency: String,
    ): CoinTickerDisplayData {
        val data = CoinTickerDisplayData.create(
            config = config,
            coinDetail = coinDetail,
            marketChartResponse = marketChartResponse,
            userCurrency = userCurrency,
        )
        coinTickerRepository.setDisplayData(widgetId = widgetId, data = data)
        return data
    }

    fun save() {
        saved = true
    }

    override fun onCleared() {
        if (!saved) {
            viewModelScope.launch(NonCancellable) {
                coinTickerRepository.clearDisplayData(widgetId)
            }
        }
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(args: CoinTickerPreviewArgs): CoinTickerPreviewViewModel
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
            val fragment =
                (viewModelContext as FragmentViewModelContext).fragment<CoinTickerPreviewFragment>()
            val args = fragment.requireArgs<CoinTickerPreviewArgs>()
            return fragment.viewModelFactory.create(args)
        }
    }
}