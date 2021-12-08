package com.rainyseason.cj.coinselect

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.navigation.findNavController
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.epoxy.VisibilityState
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.withState
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.coinselect.view.entryView
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.CoinSelectTTI
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.common.getUserErrorMessage
import com.rainyseason.cj.common.loadingView
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.common.view.retryView
import com.rainyseason.cj.common.view.settingHeaderView
import com.rainyseason.cj.data.CoinHistoryEntry
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.data.coingecko.MarketsResponseEntry
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.math.max

class CoinSelectController @AssistedInject constructor(
    @Assisted private val viewModel: CoinSelectViewModel,
    @Assisted private val resultDestination: Int,
    private val context: Context,
    private val traceManager: TraceManager,
) : AsyncEpoxyController() {

    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: CoinSelectViewModel,
            resultDestination: Int,
        ): CoinSelectController
    }

    override fun buildModels() {
        val state = withState(viewModel) { it }
        emptyView {
            id("holder")
        }
        listOf(
            ::buildRetryButton,
            ::buildHistory,
            ::buildExchanges,
            ::buildOtherSources,
            ::buildMarketDefault,
            ::buildSearchResultDefault,
            ::buildBackendEntries,
        ).forEach { builder ->
            val buildResult = builder.invoke(state)
            if (buildResult == BuildState.Stop) {
                return
            }
            if (buildResult == BuildState.StopWithLoading) {
                loadingView {
                    id("loading")
                }
            }
        }
    }

    private fun buildBackendEntries(state: CoinSelectState): BuildState {
        val backend = state.backend
        if (backend.isDefault) {
            return BuildState.Next
        }

        val data = state.backendProductMap[backend] ?: return BuildState.Next

        if (data is Fail) {
            retryView {
                id("retry")
                reason(data.error.getUserErrorMessage(context = context))
                buttonText(R.string.reload)
                onRetryClickListener { _ ->
                    viewModel.reload()
                }
            }
            return BuildState.Stop
        }

        if (data is Loading) {
            return BuildState.StopWithLoading
        }

        val list: List<BackendProduct> = data.invoke() ?: return BuildState.Next
        val keyword = state.keyword
        val filteredList = list.filter {
            it.symbol.contains(keyword, true) ||
                it.displayName.contains(keyword, true)
        }

        settingHeaderView {
            id("header_backend")
            content(backend.displayName)
        }

        filteredList.forEach { entry ->
            entryView {
                id("backend_product_${entry.uniqueId}")
                title(entry.displayName)
                subTitle("")
                iconUrl(entry.iconUrl)
                onClickListener { view ->
                    viewModel.addToHistory(
                        CoinHistoryEntry(
                            id = entry.id,
                            symbol = entry.symbol,
                            name = entry.displayName,
                            iconUrl = entry.iconUrl,
                            backend = backend
                        )
                    )
                    moveToResult(view, entry.id, entry.backend)
                }
            }
        }

        return BuildState.Next
    }

    private fun buildExchanges(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val keyword = state.keyword

        val exchanges = Backend.values().filter { it.isExchange }
            .filter { it.displayName.contains(keyword, true) }

        if (exchanges.isEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("header_exchanges")
            content(R.string.coin_select_exchange_header)
        }

        exchanges.forEach { backend ->
            entryView {
                id("backend_${backend.id}")
                title(backend.displayName)
                subTitle("")
                iconUrl(backend.iconUrl)
                onClickListener { _ ->
                    viewModel.setBackend(backend)
                    viewModel.submitNewKeyword("")
                }
            }
        }

        return BuildState.Next
    }

    private fun buildOtherSources(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val keyword = state.keyword

        val exchanges = Backend.values().filter { !it.isExchange && !it.isDefault }
            .filter { it.displayName.contains(keyword, true) }

        if (exchanges.isEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("header_other_sources")
            content(R.string.coin_select_other_source_header)
        }

        exchanges.forEach { backend ->
            entryView {
                id("backend_${backend.id}")
                title(backend.displayName)
                subTitle("")
                iconUrl(backend.iconUrl)
                onClickListener { _ ->
                    viewModel.setBackend(backend)
                    viewModel.submitNewKeyword("")
                }
            }
        }

        return BuildState.Next
    }

    private fun buildRetryButton(state: CoinSelectState): BuildState {
        val async = state.markets
        if (async is Fail) {
            if (BuildConfig.DEBUG) {
                async.error.printStackTrace()
            }
            retryView {
                id("retry")
                reason(async.error.getUserErrorMessage(context = context))
                buttonText(R.string.reload)
                onRetryClickListener { _ ->
                    viewModel.reload()
                }
            }
            return BuildState.Stop
        }

        return BuildState.Next
    }

    private fun buildHistory(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val historyEntries = state.history.invoke().orEmpty()
        if (historyEntries.isEmpty()) {
            return BuildState.Next
        }

        if (state.keyword.isNotEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("header_history")
            content(R.string.coin_history_header)
        }

        historyEntries.forEach { entry: CoinHistoryEntry ->
            entryView {
                id("history_${entry.uniqueId}")
                title(entry.name)
                if (entry.backend.isExchange) {
                    subTitle(entry.backend.displayName)
                } else {
                    if (entry.backend.isDefault) {
                        subTitle(entry.symbol.uppercase())
                    } else {
                        subTitle("${entry.symbol.uppercase()} (${entry.backend.displayName})")
                    }
                }
                iconUrl(entry.iconUrl)
                onClearClickListener { _ ->
                    viewModel.removeHistory(id = entry.id)
                }
                onClickListener { view ->
                    viewModel.addToHistory(entry)
                    moveToResult(view, coinId = entry.id, backend = entry.backend)
                }
            }
        }

        return BuildState.Next
    }

    private fun buildMarketDefault(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val keyword = state.keyword

        val async = state.markets
        if (async is Loading) {
            return BuildState.StopWithLoading
        }

        if (async is Error) {
            // build retry button
            return BuildState.Next
        }

        val marketEntries = async.invoke().orEmpty()
        val marketSearchResult = marketEntries.filter {
            it.name.contains(keyword, true) ||
                it.symbol.contains(keyword, true)
        }

        if (marketSearchResult.isEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("header_market")
            content(R.string.coin_market_header)
        }

        marketSearchResult.forEachIndexed { index, entry: MarketsResponseEntry ->
            entryView {
                id(entry.uniqueId)
                title(entry.name)
                subTitle(entry.symbol.uppercase())
                iconUrl(entry.image)
                if (index == 0) {
                    onVisibilityStateChanged { _, view, visibilityState ->
                        if (visibilityState == VisibilityState.VISIBLE) {
                            view.doOnPreDraw {
                                traceManager.endTrace(CoinSelectTTI(viewModel.id))
                            }
                        }
                    }
                }
                onClickListener { view ->
                    viewModel.addToHistory(
                        CoinHistoryEntry(
                            id = entry.id,
                            symbol = entry.symbol,
                            iconUrl = entry.image,
                            name = entry.name,
                        )
                    )
                    moveToResult(view, coinId = entry.id)
                }
            }
        }

        return BuildState.Next
    }

    private fun moveToResult(view: View, coinId: String, backend: Backend = Backend.CoinGecko) {
        view.dismissKeyboard()
        val controller = view.findNavController()
        controller.navigate(
            resultDestination,
            Bundle().apply {
                putString("coin_id", coinId)
                putString("backend_id", backend.id)
            }
        )
    }

    private fun calculateRatio(keyword: String, value: String): Double {
        if (value.contains(keyword, true)) {
            return keyword.length.toDouble() / value.length
        }
        return 0.0
    }

    private fun buildSearchResultDefault(state: CoinSelectState): BuildState {
        if (!state.backend.isDefault) {
            return BuildState.Next
        }
        val keyword = state.keyword
        if (keyword.isEmpty()) {
            // hide search result when keyword is empty
            return BuildState.Next
        }

        val async = state.list
        if (async is Loading) {
            return BuildState.StopWithLoading
        }

        if (async is Fail) {
            // build retry button
            return BuildState.Stop
        }

        val list = async.invoke().orEmpty().filter {
            it.name.contains(keyword, true) || it.symbol.contains(keyword, true)
        }

        val showInMarket = state.markets.invoke().orEmpty()
            .filter {
                it.name.contains(keyword, true) ||
                    it.symbol.contains(keyword, true)
            }
            .map { it.id }
            .toSet()

        val topList = state.markets.invoke().orEmpty().map { it.id }.toSet()

        if (list.isEmpty()) {
            // build no match view
            return BuildState.Stop
        }

        // TODO sort by market cap
        val orderedList = list.sortedWith(
            compareByDescending<CoinListEntry> {
                max(calculateRatio(keyword, it.name), calculateRatio(keyword, it.symbol))
            }.thenByDescending {
                topList.contains(it.id)
            }
        ).filter { !showInMarket.contains(it.id) }

        if (orderedList.isEmpty()) {
            return BuildState.Next
        }

        settingHeaderView {
            id("other_result")
            content(R.string.search_result_other_coin_header)
        }

        orderedList.forEach { entry: CoinListEntry ->
            entryView {
                id(entry.uniqueId)
                title(entry.name)
                subTitle(entry.symbol.uppercase())
                iconUrl(Backend.CoinGecko.iconUrl)
                onClickListener { view ->
                    viewModel.addToHistory(
                        CoinHistoryEntry(
                            id = entry.id,
                            symbol = entry.symbol,
                            iconUrl = Backend.CoinGecko.iconUrl,
                            name = entry.name,
                        )
                    )
                    moveToResult(view, coinId = entry.id)
                }
            }
        }

        return BuildState.Next
    }
}
