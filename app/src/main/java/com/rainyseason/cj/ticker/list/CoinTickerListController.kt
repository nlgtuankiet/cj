package com.rainyseason.cj.ticker.list

import android.content.Context
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.ticker.CoinTickerNavigator
import com.rainyseason.cj.ticker.list.view.coinTickerListCoinView
import com.rainyseason.cj.ticker.list.view.coinTickerListHeaderView
import com.rainyseason.cj.ticker.list.view.coinTickerListMarketView
import com.rainyseason.cj.ticker.list.view.coinTickerListSearchView

class CoinTickerListController constructor(
    private val viewModel: CoinTickerListViewModel,
    private val context: Context,
    private val navigator: CoinTickerNavigator,
) : AsyncEpoxyController() {

    fun buildSearchBox(state: CoinTickerListState): BuildState {
        val keyword = state.keyword
        coinTickerListSearchView {
            id("search-box")
            hint(R.string.coin_ticker_list_search)
            value(keyword)
            textChangeListener { newKeyword ->
                viewModel.submitNewKeyword(newKeyword)
            }
        }

        return BuildState.Next
    }

    fun buildMarket(state: CoinTickerListState): BuildState {
        val keyword = state.keyword
        if (keyword.isNotEmpty()) {
            // hide market when user search
            return BuildState.Next
        }

        val async = state.markets
        if (async is Loading) {
            // build loading
            return BuildState.Stop
        }

        if (async is Error) {
            // build retry button
            return BuildState.Next
        }

        val marketEntries = async.invoke().orEmpty()

        if (marketEntries.isEmpty()) {
            return BuildState.Next
        }


        coinTickerListHeaderView {
            id("market-header")
            content(R.string.coin_ticker_list_market_header)
        }

        marketEntries.forEach { entry ->
            coinTickerListMarketView {
                id(entry.id)
                name(entry.name)
                symbol(entry.symbol)
                iconUrl(entry.image)
            }
        }

        return BuildState.Next
    }

    fun buildSearchResult(state: CoinTickerListState): BuildState {
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

        coinTickerListHeaderView {
            id("list-header")
            content(R.string.coin_ticker_list_all_header)
        }

        if (list.isEmpty()) {
            // build no match view
            return BuildState.Stop
        }

        list.forEach { entry ->
            coinTickerListCoinView {
                id(entry.id)
                name(entry.name)
                symbol(entry.symbol)
            }
        }

        return BuildState.Next
    }

    override fun buildModels() {
        val state = withState(viewModel) { it }

        listOf(
            ::buildSearchBox,
            ::buildMarket,
            ::buildSearchResult,
        ).forEach { builder ->
            val buildResult = builder.invoke(state)
            if (buildResult == BuildState.Stop) {
                return
            }
        }
    }
}