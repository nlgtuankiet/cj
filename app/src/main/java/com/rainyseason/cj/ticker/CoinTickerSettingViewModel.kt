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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

data class CoinTickerSettingState(
    val coinId: String? = null,
    val savedWidgetConfig: Async<TickerWidgetDisplayConfig> = Uninitialized,
    val userCurrency: Async<UserCurrency> = Uninitialized,
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
) : MavericksState

class CoinTickerSettingViewModel @AssistedInject constructor(
    @Assisted private val widgetId: Int,
    private val coinTickerRepository: CoinTickerRepository,
    private val userSettingRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
) : MavericksViewModel<CoinTickerSettingState>(CoinTickerSettingState()) {

    init {
        loadUserCurrency()
        setCoinId("dogecoin")
        onEach { state ->
            maybeSaveWidgetConfig(state)
        }
    }

    private val _saveEvent = Channel<TickerWidgetDisplayConfig>(capacity = Channel.CONFLATED)
    val saveEvent = _saveEvent.receiveAsFlow()

    fun save() {
        // improvement: wait for config complete
        withState { state ->
            val config = state.savedWidgetConfig.invoke() ?: return@withState
            _saveEvent.trySend(config)
        }
    }

    fun setCoinId(id: String) {
        setState { copy(coinId = id) }
        loadCoinDetail(coinId = id)
    }

    private fun loadUserCurrency() {
        suspend {
            userSettingRepository.getCurrency()
        }.execute {
            copy(userCurrency = it)
        }
    }


    private fun loadCoinDetail(coinId: String) {
        suspend {
            coinGeckoService.getCoinDetail(coinId)
        }.execute {
            copy(coinDetailResponse = it)
        }
    }

    private fun maybeSaveWidgetConfig(state: CoinTickerSettingState) {
        val coinId = state.coinId ?: return
        val userCurrency = state.userCurrency.invoke() ?: return
        val coinDetail = state.coinDetailResponse.invoke() ?: return
        suspend {
            setWidgetConfig(
                widgetId = widgetId,
                coinId = coinId,
                userCurrency = userCurrency,
                coinDetail = coinDetail
            )
        }.execute(retainValue = CoinTickerSettingState::savedWidgetConfig) { async ->
            copy(savedWidgetConfig = async)
        }
    }

    private suspend fun setWidgetConfig(
        widgetId: Int,
        coinId: String,
        userCurrency: UserCurrency,
        coinDetail: CoinDetailResponse
    ): TickerWidgetDisplayConfig {
        val tickerWidgetDisplayConfig = TickerWidgetDisplayConfig(
            id = coinId,
            iconUrl = coinDetail.image.large,
            symbol = coinDetail.symbol,
            currentPrice = coinDetail.marketData.currentPrice[userCurrency.id]!!,
            currencySymbol = userCurrency.symbol,
            currencySymbolOnTheLeft = userCurrency.placeOnTheLeft,
            separator = userCurrency.separator,
            priceChangePercentage24h = coinDetail.marketData.priceChangePercentage24h,
            priceChangePercentage7d = coinDetail.marketData.priceChangePercentage7d,
        )
        coinTickerRepository.setConfig(widgetId = widgetId, config = tickerWidgetDisplayConfig)
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