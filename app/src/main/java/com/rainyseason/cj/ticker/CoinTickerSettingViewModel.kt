package com.rainyseason.cj.ticker

import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.local.CoinTickerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

data class CoinTickerSettingState(
    val title: String = "Hello World"
) : MavericksState

class CoinTickerSettingViewModel @AssistedInject constructor(
    @Assisted widgetId: Int,
    private val coinTickerRepository: CoinTickerRepository,
    private val coinGeckoService: CoinGeckoService,
) : MavericksViewModel<CoinTickerSettingState>(CoinTickerSettingState()) {

    init {
        Timber.d("widgetId: $widgetId")
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

        override fun initialState(viewModelContext: ViewModelContext): CoinTickerSettingState? {
            return CoinTickerSettingState()
        }
    }
}