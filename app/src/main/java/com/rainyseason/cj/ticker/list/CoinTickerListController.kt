package com.rainyseason.cj.ticker.list

import android.content.Context
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.ticker.list.view.coinTickerListHeaderView
import com.rainyseason.cj.ticker.list.view.coinTickerListMarketView

class CoinTickerListController constructor(
    private val viewModel: CoinTickerListViewModel,
    private val context: Context,
) : AsyncEpoxyController() {
    override fun buildModels() {
        val state = withState(viewModel) { it }

        val markets = state.markets

        if (!markets.complete) {
            // show loading
            return
        }

        if (markets is Fail) {
            // show reload button
            return
        }

        val marketEntries = markets.invoke().orEmpty()

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

    }
}