package com.rainyseason.cj.ticker.list

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.data.coingecko.MarketsResponseEntry
import com.rainyseason.cj.ticker.CoinTickerListFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CoinTickerListState(
    val markets: Async<List<MarketsResponseEntry>> = Uninitialized,
    val list: Async<List<CoinListEntry>> = Uninitialized,
    val keyword: String = "",
) : MavericksState

class CoinTickerListViewModel @Inject constructor(
    private val userCuRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
) : MavericksViewModel<CoinTickerListState>(CoinTickerListState()) {


    init {
        reload()
    }

    private var listJob: Job? = null
    private var marketJob: Job? = null

    private fun reload() {
        Timber.d("reload")
        listJob?.cancel()
        listJob = suspend {
            val result = coinGeckoService.getCoinList()
            println(result)
            result
        }.execute { copy(list = it) }

        marketJob?.cancel()
        marketJob = viewModelScope.launch {
            val userCurrency = userCuRepository.getCurrency()
            suspend {
                coinGeckoService.getCoinMarkets(vsCurrency = userCurrency.id, perPage = 10)
            }.execute { copy(markets = it) }
        }
    }


    companion object : MavericksViewModelFactory<CoinTickerListViewModel, CoinTickerListState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: CoinTickerListState
        ): CoinTickerListViewModel? {
            val fragment =
                (viewModelContext as FragmentViewModelContext).fragment<CoinTickerListFragment>()
            return fragment.viewModelProvider.get()
        }

        override fun initialState(viewModelContext: ViewModelContext): CoinTickerListState {
            return CoinTickerListState()
        }
    }
}