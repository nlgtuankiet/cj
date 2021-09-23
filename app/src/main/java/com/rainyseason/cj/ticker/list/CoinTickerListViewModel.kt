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
import com.rainyseason.cj.data.coingecko.getCoinListFlow
import com.rainyseason.cj.data.coingecko.getCoinMarketsFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.skip
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CoinTickerListState(
    val markets: Async<List<MarketsResponseEntry>> = Uninitialized,
    val list: Async<List<CoinListEntry>> = Uninitialized,
    val keyword: String = "",
) : MavericksState


class CoinTickerListViewModel @Inject constructor(
    private val userSettingRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
) : MavericksViewModel<CoinTickerListState>(CoinTickerListState()) {

    private val keywordDebound = MutableStateFlow("")

    init {
        reload()
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            keywordDebound.drop(1)
                .debounce(300)
                .distinctUntilChanged()
                .collect { setState { copy(keyword = it) } }
        }
    }


    fun submitNewKeyword(newKeyword: String) {
        keywordDebound.value = newKeyword.trim()
    }

    private var listJob: Job? = null
    private var marketJob: Job? = null

    fun reload() {
        listJob?.cancel()
        listJob = coinGeckoService.getCoinListFlow().execute { copy(list = it) }

        marketJob?.cancel()
        marketJob = viewModelScope.launch {
            val setting = userSettingRepository.getUserSetting()
            coinGeckoService.getCoinMarketsFlow(vsCurrency = setting.currencyCode, perPage = 1000)
            .execute { copy(markets = it) }
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