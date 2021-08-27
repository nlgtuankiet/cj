package com.rainyseason.cj.ticker

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.data.UserCurrency
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.local.CoinTickerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class CoinTickerSettingState(
    val coinId: String? = null,
    val savedDisplayData: Async<TickerWidgetDisplayData> = Uninitialized,
    val config: TickerWidgetConfig? = null,
    val savedConfig: Async<TickerWidgetConfig> = Uninitialized,
    val userCurrency: Async<UserCurrency> = Uninitialized,

    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val numberOfDecimal: Int? = null,
) : MavericksState

class CoinTickerSettingViewModel @AssistedInject constructor(
    @Assisted private val widgetId: Int,
    private val coinTickerRepository: CoinTickerRepository,
    private val userSettingRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
) : MavericksViewModel<CoinTickerSettingState>(CoinTickerSettingState()) {

    init {
        loadUserCurrency()
        loadConfig()
        loadDisplayData()
        setCoinId("dogecoin")
        onEach { state ->
            maybeSaveDisplayData(state)
        }
        onEach(CoinTickerSettingState::config) { config ->
            config?.let { maybeSaveConfig(config) }
        }
    }

    fun setNumberOfDecimal(value: String) {
        Timber.d("setNumberOfDecimal $value")
        val number = value.toIntOrNull()
        setState { copy(config = config?.copy(numberOfDecimal = number)) }
    }

    private fun loadDisplayData() {
        coinTickerRepository.getDisplayDataStream(widgetId)
            .execute { copy(savedDisplayData = it) }
    }

    private fun maybeSaveConfig(config: TickerWidgetConfig) {
        viewModelScope.launch {
            coinTickerRepository.setConfig(widgetId, config)
        }
    }

    private fun loadConfig() {
        coinTickerRepository.getConfigStream(widgetId)
            .execute { copy(savedConfig = it) }
    }

    private val _saveEvent = Channel<Unit>(capacity = Channel.CONFLATED)
    val saveEvent = _saveEvent.receiveAsFlow()

    fun save() {
        // improvement: wait for config complete
        _saveEvent.trySend(Unit)
    }

    fun setCoinId(id: String) {
        setState {
            copy(
                coinId = id,
                config = TickerWidgetConfig(
                    widgetId = widgetId,
                    coinId = id,
                    showChange24h = config?.showChange24h ?: true,
                    showChange7d = config?.showChange7d ?: true,
                    showChange14d = config?.showChange14d ?: false,
                )
            )
        }
        loadCoinDetail(coinId = id)
    }

    private fun loadUserCurrency() {
        suspend {
            userSettingRepository.getCurrency()
        }.execute {
            copy(userCurrency = it)
        }
    }

    private var loadCoinDetailJob: Job? = null
    private fun loadCoinDetail(coinId: String) {
        loadCoinDetailJob?.cancel()
        loadCoinDetailJob = suspend {
            coinGeckoService.getCoinDetail(coinId)
        }.execute {
            copy(coinDetailResponse = it)
        }
    }

    private fun maybeSaveDisplayData(state: CoinTickerSettingState) {
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
        fun create(widgetId: Int): CoinTickerSettingViewModel
    }

    companion object :
        MavericksViewModelFactory<CoinTickerSettingViewModel, CoinTickerSettingState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: CoinTickerSettingState
        ): CoinTickerSettingViewModel {
            val activity = viewModelContext.activity as CoinTickerSettingActivity
            return activity.viewModelFactory.create(activity.getWidgetId() ?: 0)
        }

        override fun initialState(viewModelContext: ViewModelContext): CoinTickerSettingState {
            return CoinTickerSettingState()
        }
    }
}