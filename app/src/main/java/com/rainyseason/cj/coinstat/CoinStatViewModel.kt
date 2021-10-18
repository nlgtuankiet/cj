package com.rainyseason.cj.coinstat

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.asDayString
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
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val marketChartResponse: Map<TimeInterval, Async<MarketChartResponse>> = emptyMap(),
    val selectedPriceRange: TimeInterval = TimeInterval.I_24H,
    val priceRange: Pair<Double, Double>? = null,
) : MavericksState

class CoinStatViewModel @AssistedInject constructor(
    @Assisted initialState: CoinStatState,
    @Assisted private val args: CoinStatArgs,
    private val coinGeckoService: CoinGeckoService,
    private val userSettingRepository: UserSettingRepository,
) : MavericksViewModel<CoinStatState>(initialState) {

    fun onSelectPriceRange(interval: TimeInterval) {
        setState { copy(selectedPriceRange = interval) }
    }

    init {
        userSettingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }
        suspend {
            coinGeckoService.getCoinDetail(args.coinId)
        }.execute { copy(coinDetailResponse = it) }

        onAsync(CoinStatState::userSetting) { userSetting ->
            listOf(
                TimeInterval.I_24H,
                TimeInterval.I_30D,
                TimeInterval.I_1Y,
            ).forEach { timeInterval ->
                suspend {
                    coinGeckoService.getMarketChart(
                        args.coinId,
                        userSetting.currencyCode,
                        timeInterval.asDayString()!!
                    )
                }.execute {
                    copy(marketChartResponse = marketChartResponse.update { put(timeInterval, it) })
                }

            }

        }

        onEach(
            CoinStatState::marketChartResponse,
            CoinStatState::selectedPriceRange
        ) { marketChartResponse, selectedPriceRange ->
            val graph = marketChartResponse[selectedPriceRange]?.invoke()?.prices
            if (graph == null || graph.isEmpty()) {
                setState { copy(priceRange = null) }
            } else {
                val low = graph.minOf { it[1] }
                val high = graph.maxOf { it[1] }
                setState { copy(priceRange = low to high) }
            }
        }
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
