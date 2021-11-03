package com.rainyseason.cj.detail

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.asDayString
import com.rainyseason.cj.common.reverseValue
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.MarketChartResponse
import com.rainyseason.cj.data.coingecko.getMarketChartWithFilter
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private typealias State = CoinDetailState

data class CoinDetailState(
    val coinDetailResponse: Async<CoinDetailResponse> = Uninitialized,
    val userSetting: Async<UserSetting> = Uninitialized,
    val marketChartResponse: Map<TimeInterval, Async<MarketChartResponse>> = emptyMap(),
    val selectedInterval: TimeInterval = TimeInterval.I_24H,
    val selectedLowHighInterval: TimeInterval = TimeInterval.I_24H,
    val lowHighPrice: Pair<Double, Double>? = null,

    val watchList: Async<List<String>> = Uninitialized,
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

    fun onSelectLowHigh(interval: TimeInterval) {
        setState { copy(selectedLowHighInterval = interval) }
    }

    private fun calculateGraphData(
        marketChartResponse: Map<TimeInterval, Async<MarketChartResponse>>,
        selectedInterval: TimeInterval,
    ): List<List<Double>> {
        val responseInterval = when (selectedInterval) {
            TimeInterval.I_1H -> TimeInterval.I_24H
            TimeInterval.I_24H -> TimeInterval.I_24H
            TimeInterval.I_7D -> TimeInterval.I_30D
            TimeInterval.I_30D -> TimeInterval.I_30D
            TimeInterval.I_90D -> TimeInterval.I_1Y
            TimeInterval.I_1Y -> TimeInterval.I_1Y
            TimeInterval.I_ALL -> TimeInterval.I_ALL
        }

        val priceGraph = marketChartResponse[responseInterval]?.invoke()
            ?.prices ?: return emptyList()

        if (priceGraph.isEmpty()) {
            return emptyList()
        }

        // TODO fine better way to filter data
        val currentTime = System.currentTimeMillis()
        val graphData = when (selectedInterval) {
            TimeInterval.I_1H -> priceGraph.filter {
                it[0] > currentTime - TimeUnit.HOURS.toMillis(1)
            }
            TimeInterval.I_24H -> priceGraph
            TimeInterval.I_7D -> priceGraph.filter {
                it[0] > currentTime - TimeUnit.DAYS.toMillis(7)
            }

            TimeInterval.I_30D -> priceGraph
            TimeInterval.I_90D -> priceGraph.filter {
                it[0] > currentTime - TimeUnit.DAYS.toMillis(90)
            }
            TimeInterval.I_1Y -> priceGraph
            TimeInterval.I_ALL -> priceGraph
        }
        if (graphData.isEmpty()) {
            return emptyList()
        }
        val isNegative = graphData.first()[1] > graphData.last()[1]
        val finalGraphData = if (isNegative && DebugFlag.POSITIVE_WIDGET.isEnable) {
            graphData.reverseValue()
        } else {
            graphData
        }
        return finalGraphData
    }

    private fun reload() {
        coinDetailJob?.cancel()
        coinDetailJob = suspend {
            coinGeckoService.getCoinDetail(args.coinId)
        }.execute { copy(coinDetailResponse = it) }

        viewModelScope.launch {
            val currencyCode = userSettingRepository.getUserSetting().currencyCode

            listOf(
                TimeInterval.I_24H,
                TimeInterval.I_30D,
                TimeInterval.I_1Y,
                TimeInterval.I_ALL,
            ).forEach { interval ->
                loadGraphJobs[interval]?.cancel()
                loadGraphJobs[interval] = suspend {
                    coinGeckoService.getMarketChartWithFilter(
                        id = args.coinId,
                        vsCurrency = currencyCode,
                        day = interval.asDayString()!!
                    )
                }.execute {
                    copy(marketChartResponse = marketChartResponse.update { set(interval, it) })
                }
            }
        }

        userSettingJob?.cancel()
        userSettingJob = userSettingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }

        watchListRepository.getWatchList()
            .execute { copy(watchList = it) }
    }

    fun onAddToWatchListClick() {
        withState { state ->
            val watchList = state.watchList.invoke() ?: return@withState
            if (state.addToWatchList is Loading) {
                return@withState
            }
            suspend {
                if (watchList.contains(args.coinId)) {
                    watchListRepository.remove(args.coinId)
                } else {
                    watchListRepository.add(args.coinId)
                }
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
