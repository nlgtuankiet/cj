package com.rainyseason.cj.detail

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.findApproxIndex
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.Watchlist
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private typealias State = CoinDetailState

data class CoinDetailState(
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val userSetting: Async<UserSetting> = Uninitialized,
    val marketChartResponse: Map<TimeInterval, Async<MarketChartResponse>> = emptyMap(),
    val selectedInterval: TimeInterval = TimeInterval.I_24H,
    val selectedLowHighInterval: TimeInterval = TimeInterval.I_24H,
    val lowHighPrice: Pair<Double, Double>? = null,

    val defaultWatchListCoins: Async<List<Coin>> = Uninitialized,
    val addToWatchList: Async<Unit> = Uninitialized,

    val graphData: List<List<Double>> = emptyList(),
    val graphChangePercent: Double? = null,

    val selectedIndex: Int? = null,
) : MavericksState

class CoinDetailViewModel @AssistedInject constructor(
    @Assisted initState: CoinDetailState,
    @Assisted private val args: CoinDetailArgs,
    private val coinGeckoService: CoinGeckoService,
    private val userSettingRepository: UserSettingRepository,
    private val watchListRepository: WatchListRepository,
) : MavericksViewModel<CoinDetailState>(initState) {

    private var coinDetailJob: Job? = null
    private val loadGraphJobs = mutableMapOf<TimeInterval, Job>()
    private var userSettingJob: Job? = null

    init {
        reload()
        onEach(
            State::marketChartResponse,
            State::selectedInterval,
        ) { marketChartResponse, selectedInterval ->
            setState {
                val graph = calculateGraphData(marketChartResponse, selectedInterval)
                val changePercent = if (graph.isEmpty()) {
                    null
                } else {
                    val start = graph.first()[1]
                    val end = graph.last()[1]
                    val percent = 100 * (end - start) / start
                    percent
                }
                copy(graphData = graph, graphChangePercent = changePercent)
            }
        }

        onEach(
            State::marketChartResponse,
            State::selectedLowHighInterval
        ) { marketChartResponse, selectedLowHighInterval ->
            val graph = calculateGraphData(marketChartResponse, selectedLowHighInterval)
            if (graph.isEmpty()) {
                setState { copy(lowHighPrice = null) }
            } else {
                val low = graph.minOf { it[1] }
                val high = graph.maxOf { it[1] }
                setState { copy(lowHighPrice = low to high) }
            }
        }
    }

    private fun maybeLoadMarketChart(code: String, interval: TimeInterval) {
        withState { state ->
            val actualInterval = when (interval) {
                TimeInterval.I_1H -> TimeInterval.I_24H
                else -> interval
            }
            val previousAsync = state.marketChartResponse[actualInterval]
            if (previousAsync is Success || previousAsync is Loading) {
                return@withState
            }
            loadGraphJobs[actualInterval]?.cancel()
            loadGraphJobs[actualInterval] = suspend {
                coinGeckoService.getMarketChart(
                    id = args.coin.id,
                    vsCurrency = code,
                    day = actualInterval.asDayString()!!
                )
            }.execute {
                copy(
                    marketChartResponse = marketChartResponse.update { set(actualInterval, it) }
                )
            }
        }
    }

    fun onSelectLowHigh(interval: TimeInterval) {
        setState { copy(selectedLowHighInterval = interval) }
    }

    private fun calculateGraphData(
        marketChartResponse: Map<TimeInterval, Async<MarketChartResponse>>,
        selectedInterval: TimeInterval,
    ): List<List<Double>> {
        val responseInterval = when (selectedInterval) {
            TimeInterval.I_1H -> TimeInterval.I_24H
            else -> selectedInterval
        }

        val priceGraph = marketChartResponse[responseInterval]?.invoke()
            ?.prices ?: return emptyList()

        if (priceGraph.isEmpty()) {
            return emptyList()
        }

        val graphData = when (selectedInterval) {
            TimeInterval.I_1H -> {
                if (priceGraph.isEmpty()) {
                    return emptyList()
                }
                val lastTime = priceGraph.lastOrNull()?.get(0) ?: return emptyList()
                val startTime = lastTime - selectedInterval.toMilis()
                val index = priceGraph.findApproxIndex(startTime)
                priceGraph.subList(index, priceGraph.size)
            }
            else -> priceGraph
        }
        return graphData
    }

    /**
     * TODO respect currency code
     */
    private var onEachSelectedInterval: Job? = null
    private fun reload() {
        watchListRepository.getWatchlistCollectionFlow()
            .map { collection ->
                collection.list.firstOrNull { watchlist -> watchlist.id == Watchlist.DEFAULT_ID }
                    ?.coins.orEmpty()
            }
            .execute { copy(defaultWatchListCoins = it) }

        if (args.coin.backend != Backend.CoinGecko) {
            return
        }

        coinDetailJob?.cancel()
        coinDetailJob = suspend {
            coinGeckoService.getCoinDetail(args.coin.id)
        }.execute { copy(coinDetailResponse = it) }

        viewModelScope.launch {
            val currencyCode = userSettingRepository.getUserSetting().currencyCode
            maybeLoadMarketChart(currencyCode, TimeInterval.I_24H)
            maybeLoadMarketChart(currencyCode, TimeInterval.I_30D)
            maybeLoadMarketChart(currencyCode, TimeInterval.I_1Y)
            onEachSelectedInterval?.cancel()
            onEachSelectedInterval = onEach(CoinDetailState::selectedInterval) { interval ->
                maybeLoadMarketChart(currencyCode, interval)
            }
        }

        userSettingJob?.cancel()
        userSettingJob = userSettingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }
    }

    fun onAddToWatchListClick() {
        withState { state ->
            if (state.addToWatchList is Loading) {
                return@withState
            }
            suspend {
                watchListRepository.addOrRemove(args.coin)
            }.execute { copy(addToWatchList = it) }
        }
    }

    fun onIntervalClick(interval: TimeInterval) {
        setState { copy(selectedInterval = interval) }
    }

    fun setDataTouchIndex(index: Int?) {
        setState { copy(selectedIndex = index) }
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
