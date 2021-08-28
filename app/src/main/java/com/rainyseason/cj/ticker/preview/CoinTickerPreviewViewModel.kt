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
import kotlinx.coroutines.launch
import timber.log.Timber

data class CoinTickerPreviewState(
    val savedDisplayData: Async<TickerWidgetDisplayData> = Uninitialized,
    val config: TickerWidgetConfig? = null,
    val savedConfig: Async<TickerWidgetConfig> = Uninitialized,
    val userCurrency: Async<UserCurrency> = Uninitialized,
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val numberOfDecimal: Int? = null,
) : MavericksState

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
            showChange24h = true,
            showChange7d = true,
            showChange14d = true,
            numberOfPriceDecimal = 1,
            numberOfChangePercentDecimal = 1
        )

        coinTickerRepository.setConfig(widgetId, config)
    }

    fun setNumberOfDecimal(value: String) {
        Timber.d("setNumberOfDecimal $value")
        val number = value.toIntOrNull()?.coerceAtLeast(0)
        setState { copy(config = config?.copy(numberOfPriceDecimal = number)) }
    }

    fun setNumberOfChangePercentDecimal(value: String) {
        Timber.d("setNumberOfChangePercentDecimal $value")
        val number = value.toIntOrNull()?.coerceAtLeast(0)
        setState { copy(config = config?.copy(numberOfChangePercentDecimal = number)) }
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

    private fun maybeSaveDisplayData(state: CoinTickerPreviewState) {
        val userCurrency = state.userCurrency.invoke() ?: return
        val coinDetail = state.coinDetailResponse.invoke() ?: return
        val config = state.savedConfig.invoke() ?: return

        if (!config.isComplete) {
            return
        }

        viewModelScope.launch {
            setWidgetData(
                widgetId = widgetId,
                userCurrency = userCurrency,
                coinDetail = coinDetail,
            )
        }
    }

    private suspend fun setWidgetData(
        widgetId: Int,
        userCurrency: UserCurrency,
        coinDetail: CoinDetailResponse,
    ): TickerWidgetDisplayData {
        val tickerWidgetDisplayConfig = TickerWidgetDisplayData(
            iconUrl = coinDetail.image.large,
            symbol = coinDetail.symbol,
            name = coinDetail.name,
            price = coinDetail.marketData.currentPrice[userCurrency.id]!!,
            change24hPercent = coinDetail.marketData.priceChangePercentage24h,
            change7dPercent = coinDetail.marketData.priceChangePercentage24h,
            change14dPercent = coinDetail.marketData.priceChangePercentage14d,
        )
        coinTickerRepository.setDisplayData(widgetId = widgetId, data = tickerWidgetDisplayConfig)
        return tickerWidgetDisplayConfig
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