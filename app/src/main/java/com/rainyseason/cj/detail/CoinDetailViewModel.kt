package com.rainyseason.cj.detail

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


data class CoinDetailState(
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val userSetting: Async<UserSetting> = Uninitialized,
    val marketChartResponse: Map<TimeInterval, Async<MarketChartResponse>> = emptyMap(),
    val selectedInterval: TimeInterval = TimeInterval.I_24H,
) : MavericksState

class CoinDetailViewModel @AssistedInject constructor(
    @Assisted initState: CoinDetailState,
    @Assisted private val args: CoinDetailArgs,
    private val coinGeckoService: CoinGeckoService,
    private val userSettingRepository: UserSettingRepository,
) : MavericksViewModel<CoinDetailState>(initState) {

    private var coinDetailJob: Job? = null
    private val loadGraphJobs = mutableMapOf<TimeInterval, Job>()
    private var userSettingJob: Job? = null

    init {
        reload()
    }

    private fun reload() {
        coinDetailJob?.cancel()
        coinDetailJob = suspend {
            coinGeckoService.getCoinDetail(args.coinId)
        }.execute { copy(coinDetailResponse = it) }

        viewModelScope.launch {
            val currencyCode = userSettingRepository.getUserSetting().currencyCode

            listOf(
                TimeInterval.I_24H to 1,
                TimeInterval.I_30D to 30,
                TimeInterval.I_1Y to 365,
            ).forEach { (interval, days) ->
                loadGraphJobs[interval]?.cancel()
                loadGraphJobs[interval] = suspend {
                    coinGeckoService.getMarketChart(
                        id = args.coinId,
                        vsCurrency = currencyCode,
                        day = days
                    )
                }.execute {
                    copy(marketChartResponse = marketChartResponse.update { set(interval, it) })
                }
            }
        }

        userSettingJob?.cancel()
        userSettingJob = userSettingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }
    }

    fun onIntervalClick(interval: TimeInterval) {
        setState { copy(selectedInterval = interval) }
    }


    @AssistedFactory
    interface Factory {
        fun create(
            initState: CoinDetailState,
            args: CoinDetailArgs,
        ): CoinDetailViewModel
    }

    companion object : MavericksViewModelFactory<CoinDetailViewModel, CoinDetailState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: CoinDetailState,
        ): CoinDetailViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<CoinDetailFragment>()
            return fragment.viewModelFactory.create(state, fragment.args)
        }
    }
}