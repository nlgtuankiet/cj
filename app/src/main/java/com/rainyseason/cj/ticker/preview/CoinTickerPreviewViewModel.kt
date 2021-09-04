package com.rainyseason.cj.ticker.preview

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.data.UserCurrency
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.ticker.TickerWidgetConfig
import com.rainyseason.cj.ticker.TickerWidgetDisplayData
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

data class CoinTickerPreviewState(
    val savedDisplayData: Async<TickerWidgetDisplayData> = Uninitialized,
    val savedConfig: Async<TickerWidgetConfig> = Uninitialized,
    val userCurrency: Async<UserCurrency> = Uninitialized,
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val numberOfDecimal: Int? = null,
) : MavericksState {
    val config: TickerWidgetConfig?
        get() = savedConfig.invoke()
}

class CoinTickerPreviewViewModel @AssistedInject constructor(
    @Assisted private val args: CoinTickerPreviewArgs,
    private val coinTickerRepository: CoinTickerRepository,
    private val userSettingRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
) : MavericksViewModel<CoinTickerPreviewState>(CoinTickerPreviewState()) {

    private val widgetId = args.widgetId

    init {
        loadUserCurrency()
        loadConfig()
        loadDisplayData()
        loadCoinDetail()
        onEach { state ->
            maybeSaveDisplayData(state)
        }
        onEach(CoinTickerPreviewState::config) { config ->
            config?.let { maybeSaveConfig(config) }
        }
        viewModelScope.launch {
            saveInitialConfig()
        }
    }


    private suspend fun saveInitialConfig() {
        // load last config
        val config = TickerWidgetConfig(
            widgetId = widgetId,
            coinId = args.coinId,
            numberOfPriceDecimal = null,
            numberOfChangePercentDecimal = 1
        )

        coinTickerRepository.setConfig(widgetId, config)
    }

    private fun updateConfig(block: TickerWidgetConfig.() -> TickerWidgetConfig) {
        withState { state ->
            val config = state.savedConfig.invoke()
            if (config != null) {
                val newConfig = block.invoke(config)
                maybeSaveConfig(newConfig)
            }
        }
    }

    fun setMarketCapChangeInterval(value: String) {
        updateConfig { copy(marketCapChangeInterval = value) }
    }

    fun setPriceChangeInterval(value: String) {
        updateConfig { copy(priceChangeInterval = value) }
    }

    fun setBottomContentType(type: String) {
        updateConfig { copy(bottomContentType = type) }
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

    private fun maybeSaveConfig(config: TickerWidgetConfig) {
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
        val config = state.savedConfig.invoke() ?: return

        viewModelScope.launch {
            setWidgetData(
                userCurrency = userCurrency,
                coinDetail = coinDetail,
            )
        }
    }

    private suspend fun setWidgetData(
        userCurrency: UserCurrency,
        coinDetail: CoinDetailResponse,
    ): TickerWidgetDisplayData {
        val data = TickerWidgetDisplayData.create(
            userCurrency = userCurrency,
            coinDetail = coinDetail
        )
        coinTickerRepository.setDisplayData(widgetId = widgetId, data = data)
        return data
    }

    override fun onCleared() {
        viewModelScope.launch(NonCancellable) {
            coinTickerRepository.clearAllData(args.widgetId)
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
            state: CoinTickerPreviewState
        ): CoinTickerPreviewViewModel {
            val fragment =
                (viewModelContext as FragmentViewModelContext).fragment<CoinTickerPreviewFragment>()
            val args = fragment.requireArgs<CoinTickerPreviewArgs>()
            return fragment.viewModelFactory.create(args)
        }
    }
}