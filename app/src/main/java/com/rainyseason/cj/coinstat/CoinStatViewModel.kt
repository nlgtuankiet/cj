package com.rainyseason.cj.coinstat

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.MarketChartResponse
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

data class CoinStatState(
    val userSetting: Async<UserSetting> = Uninitialized,
    val coinDetail: Async<CoinDetailResponse> = Uninitialized,
    val marketChart: Map<TimeInterval, Async<MarketChartResponse>> = emptyMap()
) : MavericksState

class CoinStatViewModel @AssistedInject constructor(
    @Assisted initialState: CoinStatState,
    @Assisted private val args: CoinStatArgs,
    private val coinGeckoService: CoinGeckoService,
    private val userSettingRepository: UserSettingRepository,
) : MavericksViewModel<CoinStatState>(initialState) {

    init {
        userSettingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }
        suspend {
            coinGeckoService.getCoinDetail(args.coinId)
        }.execute { copy(coinDetail = it) }

        suspend {
            coinGeckoService.getMarketChart(args.coinId, "usd", "1")
        }.execute { copy(marketChart = marketChart.update { put(TimeInterval.I_24H, it) }) }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initialState: CoinStatState,
            args: CoinStatArgs,
        ): CoinStatViewModel
    }

    companion object : MavericksViewModelFactory<CoinStatViewModel, CoinStatState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: CoinStatState
        ): CoinStatViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<CoinStatFragment>()
            return fragment.viewModelFactory.create(state, fragment.args)
        }
    }
}
