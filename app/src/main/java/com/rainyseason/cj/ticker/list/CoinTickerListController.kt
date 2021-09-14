package com.rainyseason.cj.ticker.list

import android.content.Context
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.getUserErrorMessage
import com.rainyseason.cj.common.loadingView
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.common.view.retryView
import com.rainyseason.cj.common.view.settingHeaderView
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.ticker.CoinTickerNavigator
import com.rainyseason.cj.ticker.list.view.coinTickerListCoinView
import com.rainyseason.cj.ticker.list.view.coinTickerListMarketView
import kotlin.math.max

class CoinTickerListController constructor(
    private val context: Context,
    private val viewModel: CoinTickerListViewModel,
    private val navigator: CoinTickerNavigator,
) : AsyncEpoxyController() {
    override fun buildModels() {
        val state = withState(viewModel) { it }
        emptyView {
            id("holder")
        }
        listOf(
            ::buildRetryButton,
            ::buildMarket,
            ::buildSearchResult,
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

    private fun buildRetryButton(state: CoinTickerListState): BuildState {
        val async = state.markets
        if (async is Fail) {
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

    private fun buildMarket(state: CoinTickerListState): BuildState {
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
            it.name.contains(keyword, true)
                    || it.symbol.contains(keyword, true)
        }

        if (marketSearchResult.isEmpty()) {
            return BuildState.Next
        }

        marketSearchResult.forEach { entry ->
            coinTickerListMarketView {
                id(entry.id)
                name(entry.name)
                symbol(entry.symbol)
                iconUrl(entry.image)
                onClickListener { _ ->
                    navigator.moveToPreview(coinId = entry.id)
                }
            }
        }

        return BuildState.Next
    }

    private fun calculateRatio(keyword: String, value: String): Double {
        if (value.contains(keyword, true)) {
            return keyword.length.toDouble() / value.length
        }
        return 0.0
    }

    private fun buildSearchResult(state: CoinTickerListState): BuildState {
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
                it.name.contains(keyword, true)
                        || it.symbol.contains(keyword, true)
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

        orderedList.forEach { entry ->
            coinTickerListCoinView {
                id(entry.id)
                name(entry.name)
                symbol(entry.symbol)
                onClickListener { _ ->
                    navigator.moveToPreview(coinId = entry.id)
                }
            }
        }

        return BuildState.Next
    }

}