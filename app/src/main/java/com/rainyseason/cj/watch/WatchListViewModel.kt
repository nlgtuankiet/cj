package com.rainyseason.cj.watch

import android.content.Context
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.realtimeFlowOf
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.data.coingecko.MarketChartResponse
import com.rainyseason.cj.data.coingecko.MarketsResponseEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections

private typealias State = WatchListState

data class WatchListState(
    val page: Int = 0,
    val userSetting: Async<UserSetting> = Uninitialized,
    val watchList: Async<List<String>> = Uninitialized,
    val watchEntryDetail: Map<String, Async<CoinDetailResponse>> = emptyMap(),
    val watchEntryMarket: Map<String, Async<MarketChartResponse>> = emptyMap(),
    val coinList: Async<List<CoinListEntry>> = Uninitialized,
    val coinMarket: Async<List<MarketsResponseEntry>> = Uninitialized,
    val keyword: String = "",
    val addTasks: Map<String, Async<*>> = emptyMap(),
    val isInEditMode: Boolean = false,
) : MavericksState

@OptIn(FlowPreview::class)
class WatchListViewModel @AssistedInject constructor(
    @Assisted state: WatchListState,
    private val coinGeckoService: CoinGeckoService,
    private val userSettingRepository: UserSettingRepository,
    private val watchListRepository: WatchListRepository,
    private val context: Context,
) : MavericksViewModel<WatchListState>(state) {
    private val wachEntryDetailJob: MutableMap<String, Job> =
        Collections.synchronizedMap(mutableMapOf())
    private val wachEntryMarketlJob: MutableMap<String, Job> =
        Collections.synchronizedMap(mutableMapOf())
    private var coinListJob: Job? = null
    private var userSettingJob: Job? = null
    private var watchListJob: Job? = null
    private var loadWatchListEntriesJob: Job? = null
    private var marketJob: Job? = null
    private val keywordDeboucer = MutableStateFlow("")

    init {
        reload()
    }

    fun reload() {
        coinListJob?.cancel()
        coinListJob = context.realtimeFlowOf {
            coinGeckoService.getCoinList()
        }.execute {
            copy(coinList = it)
        }

        marketJob?.cancel()
        marketJob = context.realtimeFlowOf {
            val currencyCode = userSettingRepository.getUserSetting().currencyCode
            coinGeckoService.getCoinMarkets(
                vsCurrency = currencyCode,
                perPage = 250,
                page = 1,
                sparkline = false
            )
        }.execute {
            copy(coinMarket = it)
        }

        userSettingJob?.cancel()
        userSettingJob = userSettingRepository.getUserSettingFlow()
            .execute {
                copy(userSetting = it)
            }

        watchListJob?.cancel()
        watchListJob = watchListRepository.getLegacyWatchlistCoinIds()
            .execute {
                copy(watchList = it)
            }

        loadWatchListEntriesJob?.cancel()
        loadWatchListEntriesJob = onEach(
            State::userSetting,
            State::watchList,
        ) { userSetting, watchList ->
            if (userSetting is Success && watchList is Success) {
                val currency = userSetting.invoke().currencyCode
                val watchListIds = watchList.invoke()
                withState { state ->
                    watchListIds.forEach { coinId ->
                        val detailAsync = state.watchEntryDetail[coinId]
                        val marketAsync = state.watchEntryMarket[coinId]
                        val needLoad = listOf(detailAsync, marketAsync)
                            .any { it == null || it is Fail }
                        if (needLoad) {
                            loadWatchEntry(coinId, currency)
                        }
                    }
                    // cancel all job that is not in the watch list
                    state.watchEntryDetail.keys.filter { !watchListIds.contains(it) }
                        .forEach { wachEntryDetailJob.remove(it)?.cancel() }
                    state.watchEntryMarket.keys.filter { !watchListIds.contains(it) }
                        .forEach { wachEntryMarketlJob.remove(it)?.cancel() }

                    setState {
                        copy(
                            watchEntryDetail = watchEntryDetail
                                .filter { watchListIds.contains(it.key) },
                            watchEntryMarket = watchEntryMarket
                                .filter { watchListIds.contains(it.key) },
                        )
                    }
                }
            }
        }
    }

    private fun loadWatchEntry(id: String, currencyCode: String) {
        Timber.d("loadWatchEntry $id")
        wachEntryDetailJob.remove(id)?.cancel()
        wachEntryDetailJob[id] = context.realtimeFlowOf {
            coinGeckoService.getCoinDetail(id)
        }.execute {
            copy(
                watchEntryDetail = watchEntryDetail.update { put(id, it) }
            )
        }

        wachEntryMarketlJob.remove(id)?.cancel()
        wachEntryMarketlJob[id] = context.realtimeFlowOf {
            coinGeckoService.getMarketChart(id, currencyCode, "1")
        }.execute {
            copy(
                watchEntryMarket = watchEntryMarket.update { put(id, it) }
            )
        }
    }

    fun onRemoveClick(id: String) {
        viewModelScope.launch {
            watchListRepository.remove(Coin(id))
        }
    }

    fun onAddClick(id: String) {
        viewModelScope.launch {
            withState { state ->
                if (state.addTasks[id] is Loading) {
                    return@withState
                }
                suspend {
                    val coin = Coin(id)
                    watchListRepository.addOrRemove(coin)
                }.execute { copy(addTasks = addTasks.update { put(id, it) }) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onClear")
    }

    init {
        viewModelScope.launch {
            keywordDeboucer.debounce(300)
                .collect { setState { copy(keyword = it) } }
        }
    }

    fun onKeywordChange(value: String) {
        keywordDeboucer.value = value.trim()
    }

    fun exitEditMode() {
        setState { copy(isInEditMode = false) }
    }

    fun switchEditMode() {
        setState { copy(isInEditMode = !isInEditMode) }
    }

    fun drag(fromId: String, toId: String) {
        viewModelScope.launch {
            watchListRepository.drag(Coin(fromId), Coin(toId))
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
