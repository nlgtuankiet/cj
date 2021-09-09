package com.rainyseason.cj.ticker.preview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.UserCurrency
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
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class CoinTickerPreviewState(
    val savedDisplayData: Async<CoinTickerDisplayData> = Uninitialized,
    val savedConfig: Async<CoinTickerConfig> = Uninitialized,
    val userCurrency: Async<UserCurrency> = Uninitialized,
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

    init {
        loadUserCurrency()
        loadConfig()
        loadDisplayData()
        ChangeInterval.ALL_PRICE_INTERVAL.forEach {
            loadGraph(it)
        }
        loadCoinDetail()

        onEach { state ->
            maybeSaveDisplayData(state)
        }

        onEach(CoinTickerPreviewState::config) { config ->
            config?.let { maybeSaveConfig(config) }
            Timber.d("config: $config")
        }

        viewModelScope.launch {
            saveInitialConfig()
        }
    }


    private suspend fun saveInitialConfig() {
        // load last config
        val config = CoinTickerConfig(
            widgetId = widgetId,
            coinId = args.coinId,
            numberOfPriceDecimal = null,
            numberOfChangePercentDecimal = 1
        )

        coinTickerRepository.setConfig(widgetId, config)
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
        suspend {
            userSettingRepository.getCurrency()
        }.execute {
            copy(userCurrency = it)
        }
    }

    private fun loadGraph(interval: String) {
        val day = when (interval) {
            ChangeInterval._7D -> 7
            ChangeInterval._14D -> 14
            ChangeInterval._30D -> 30
            ChangeInterval._1Y -> 365
            else -> 1
        }
        loadGraphJobs[interval]?.cancel()
        loadGraphJobs[interval] = suspend {
            coinGeckoService.getMarketChart(id = args.coinId, vsCurrency = "usd", day)
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
                userCurrency = userCurrency,
                coinDetail = coinDetail,
                marketChartResponse = state.marketChartResponse.mapValues { it.value.invoke() },
            )
        }
    }

    private suspend fun setWidgetData(
        config: CoinTickerConfig,
        userCurrency: UserCurrency,
        coinDetail: CoinDetailResponse,
        marketChartResponse: Map<String, MarketChartResponse?>,
    ): CoinTickerDisplayData {
        val data = CoinTickerDisplayData.create(
            config = config,
            userCurrency = userCurrency,
            coinDetail = coinDetail,
            marketChartResponse = marketChartResponse
        )
        coinTickerRepository.setDisplayData(widgetId = widgetId, data = data)
        return data
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
            state: CoinTickerPreviewState
        ): CoinTickerPreviewViewModel {
            val fragment =
                (viewModelContext as FragmentViewModelContext).fragment<CoinTickerPreviewFragment>()
            val args = fragment.requireArgs<CoinTickerPreviewArgs>()
            return fragment.viewModelFactory.create(args)
        }
    }
}