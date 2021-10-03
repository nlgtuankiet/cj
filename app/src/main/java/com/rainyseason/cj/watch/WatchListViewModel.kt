package com.rainyseason.cj.watch

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.MarketsResponseEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class WatchListState(
    val page: Int = 0,
    val markets: Async<List<MarketsResponseEntry>> = Uninitialized,
    val userSetting: Async<UserSetting> = Uninitialized,
) : MavericksState


class WatchListViewModel @AssistedInject constructor(
    @Assisted state: WatchListState,
    private val coinGeckoService: CoinGeckoService,
    private val userSettingRepository: UserSettingRepository,
) : MavericksViewModel<WatchListState>(state) {


    init {
        reload()
    }

    private var marketJob: Job? = null
    private var userSettingJob: Job? = null

    private fun reload() {
        marketJob?.cancel()
        marketJob = getMarketFlows().execute {
            copy(markets = it)
        }

        userSettingJob?.cancel()
        userSettingJob = userSettingRepository.getUserSettingFlow()
            .execute {
                copy(userSetting = it)
            }
    }


    private fun getMarketFlows(): Flow<List<MarketsResponseEntry>> {
        val list = mutableListOf<MarketsResponseEntry>()
        return flow {
            val currency = userSettingRepository.getUserSetting().currencyCode
            repeat(4) { page ->
                val coinMarkets = coinGeckoService.getCoinMarkets(
                    vsCurrency = currency,
                    perPage = 250,
                    page = page + 1,
                    sparkline = true,
                )
                list.addAll(coinMarkets)
                emit(list.toList())
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(state: WatchListState): WatchListViewModel
    }

    companion object : MavericksViewModelFactory<WatchListViewModel, WatchListState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: WatchListState,
        ): WatchListViewModel {
            return (viewModelContext as FragmentViewModelContext).fragment<WatchListFragment>()
                .viewModelFatory.create(state)
        }
    }
}