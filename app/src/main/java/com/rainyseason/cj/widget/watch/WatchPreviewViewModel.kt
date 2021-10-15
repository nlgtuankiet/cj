package com.rainyseason.cj.widget.watch

import android.appwidget.AppWidgetManager
import android.os.Parcelable
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.R
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.getWidgetId
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

data class WatchPreviewState(
    val savedDisplayData: Async<WatchDisplayData> = Uninitialized,
    val savedConfig: Async<WatchConfig> = Uninitialized,
    val watchlist: Async<List<String>> = Uninitialized,
    val userSetting: Async<UserSetting> = Uninitialized,
    val coinDetail: Map<String, Async<CoinDetailResponse>> = emptyMap(),
    val coinMarket: Map<String, Async<MarketChartResponse>> = emptyMap(),
    val previewScale: Double? = null,
    val scalePreview: Boolean = true,
) : MavericksState {
    val config: WatchConfig?
        get() = savedConfig.invoke()

    val displayData: WatchDisplayData?
        get() = savedDisplayData.invoke()
}

@Parcelize
data class WatchPreviewArgs(
    val widgetId: Int,
) : Parcelable

// for each coinid we need to load
// coin detail
// coin market (each change interval and user currency)
class WatchPreviewViewModel @AssistedInject constructor(
    @Assisted val initState: WatchPreviewState,
    @Assisted val args: WatchPreviewArgs,
    private val watchWidgetRepository: WatchWidgetRepository,
    private val watchListRepository: WatchListRepository,
    private val userSettingRepository: UserSettingRepository,
    private val coinGeckoService: CoinGeckoService,
    private val appWidgetManager: AppWidgetManager,
) : MavericksViewModel<WatchPreviewState>(initState) {
    private var saved = false

    private var loadCoinDetailJob: Job? = null
    private val loadCoinDetailJobs = mutableMapOf<String, Job>()

    init {
        loadDisplayData()
        loadWatchList()
        loadUserSetting()

        onEach { state ->
            maybeSaveDisplayData(state)
        }

        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            saveInitialConfig()
        }
        loadCoinDetail()
        loadEachEntryData()
    }


    private fun loadCoinDetail() {
        loadCoinDetailJob?.cancel()
        loadCoinDetailJob = onAsync(
            WatchPreviewState::watchlist
        ) { watchlist ->
            loadCoinDetailJobs.values.forEach { it.cancel() }
            setState { copy(coinDetail = emptyMap()) }
            // TODO replace with widget type
            watchlist.take(3).forEach { coinId ->
                loadCoinDetailJobs[coinId] = suspend {
                    coinGeckoService.getCoinDetail(coinId)
                }.execute { copy(coinDetail = coinDetail.update { put(coinId, it) }) }
            }
        }
    }

    private fun loadUserSetting() {
        userSettingRepository.getUserSettingFlow()
            .execute { copy(userSetting = it) }
    }

    private var loadEachEntryDataJob: Job? = null
    private fun loadEachEntryData() {
        loadEachEntryDataJob?.cancel()
        loadEachEntryDataJob = viewModelScope.launch {
            stateFlow
                .mapNotNull { state ->
                    val userSetting = state.userSetting.invoke()
                    val watchlist = state.watchlist.invoke()
                    val config = state.config
                    if (userSetting == null || watchlist == null || config == null) {
                        null
                    } else {
                        // TODO replace with widget type
                        Triple(userSetting.currencyCode, config.interval, watchlist.take(3))
                    }
                }
                .distinctUntilChanged()
                .collect { (currencyCode, interval, watchlist) ->
                    loadCoinMarket(currencyCode, interval, watchlist)
                }
        }
    }

    private val loadCoinMarketJobs = mutableMapOf<String, Job>()
    private fun loadCoinMarket(
        currencyCode: String,
        interval: TimeInterval,
        watchlist: List<String>
    ) {
        loadCoinMarketJobs.values.forEach { it.cancel() }
        loadCoinMarketJobs.clear()
        setState { copy(coinMarket = emptyMap()) }
        watchlist.forEach { coinId ->
            suspend {
                coinGeckoService.getMarketChart(coinId, currencyCode, interval.asDayString()!!)
            }.execute {
                copy(coinMarket = coinMarket.update { put(coinId, it) })
            }
        }
    }

    // TODO only save when something changed
    private suspend fun maybeSaveDisplayData(state: WatchPreviewState) {
        val watchlist = state.watchlist.invoke() ?: return
        val currencyCode = state.userSetting.invoke()?.currencyCode ?: return
        val entries = watchlist.map { coinId ->
            val coinDetail = state.coinDetail[coinId]?.invoke()
                ?: return@map WatchDisplayEntry(coinId, null)
            val coinMarket = state.coinMarket[coinId]?.invoke()
            val priceChart = coinMarket?.prices?.filter { it.size == 2 }?.takeIf { it.size >= 2 }
            WatchDisplayEntry(
                coinId = coinId,
                content = WatchDisplayEntryContent(
                    symbol = coinDetail.symbol,
                    name = coinDetail.name,
                    graph = priceChart,
                    price = coinDetail.marketData.currentPrice[currencyCode]!!,
                    changePercent = priceChart?.changePercent()
                )
            )
        }
        val data = WatchDisplayData(entries)
        watchWidgetRepository.setDisplayData(args.widgetId, data)
    }

    private fun loadWatchList() {
        watchListRepository.getWatchList()
            .execute { copy(watchlist = it) }
    }

    fun switchScalePreview() {
        setState { copy(scalePreview = !scalePreview) }
    }

    private suspend fun saveInitialConfig() {
        val userSetting = userSettingRepository.getUserSetting()
        val defaultLayout = appWidgetManager.getAppWidgetInfo(args.widgetId)?.initialLayout
            ?: R.layout.widget_watch_4x2_frame
        val layout = WatchWidgetLayout.fromDefaultLayout(defaultLayout)
        val config = WatchConfig(
            widgetId = args.widgetId,
            interval = TimeInterval.I_24H,
            currency = userSetting.currencyCode,
            layout = layout
        )
        watchWidgetRepository.setConfig(args.widgetId, config)
        setState { copy(previewScale = layout.previewScale) }
        loadConfig()
    }

    private fun loadDisplayData() {
        watchWidgetRepository.getDisplayDataStream(args.widgetId)
            .execute { copy(savedDisplayData = it) }
    }

    private var loadConfigJob: Job? = null
    private fun loadConfig() {
        loadConfigJob?.cancel()
        loadConfigJob = watchWidgetRepository.getConfigStream(args.widgetId)
            .execute { copy(savedConfig = it) }
    }

    override fun onCleared() {
        if (!saved) {
            viewModelScope.launch(NonCancellable) {
                watchWidgetRepository.clearDisplayData(args.widgetId)
            }
        }
        super.onCleared()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initState: WatchPreviewState,
            args: WatchPreviewArgs,
        ): WatchPreviewViewModel
    }

    companion object : MavericksViewModelFactory<WatchPreviewViewModel, WatchPreviewState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: WatchPreviewState
        ): WatchPreviewViewModel {
            val fragment = (viewModelContext as FragmentViewModelContext)
                .fragment<WatchPreviewFragment>()
            val factory = fragment.viewModelFactory
            val widgetId = fragment.requireActivity().intent.extras?.getWidgetId()
                ?: throw IllegalArgumentException("missing widget id")
            return factory.create(state, WatchPreviewArgs(widgetId))
        }
    }
}