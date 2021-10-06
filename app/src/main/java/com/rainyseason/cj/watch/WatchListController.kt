package com.rainyseason.cj.watch

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.watch.view.WatchEntryView
import com.rainyseason.cj.watch.view.watchEntrySeparatorView
import com.rainyseason.cj.watch.view.watchEntryView
import com.rainyseason.cj.watch.view.watchHeaderView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.math.max

class WatchListController @AssistedInject constructor(
    @Assisted val viewModel: WatchListViewModel,
) : AsyncEpoxyController() {
    override fun buildModels() {
        emptyView { id("holder") }
        val state = withState(viewModel) { it }

        buildWatchList(state)
        buildSearchResult(state)
    }

    private fun buildSearchResult(state: WatchListState): BuildState {
        val watchList = state.watchList.invoke() ?: return BuildState.Next
        val watchListIds = watchList.toSet()
        val coinMarket = state.coinMarket.invoke() ?: return BuildState.Next
        val currencyCode = state.userSetting.invoke()?.currencyCode ?: return BuildState.Next
        val coinList = state.coinList.invoke().orEmpty()

        val keyword = state.keyword
        if (keyword.isEmpty()) {
            return BuildState.Next
        }

        watchHeaderView {
            id("header_watch_list_symbol")
            header(R.string.watch_list_symbol_header)
        }

        var shouldBuildSeparator: Boolean = false

        val coinMarketToRender = coinMarket
            .filter { it.id !in watchListIds }
            .filter {
                it.name.contains(keyword, true)
                        || it.symbol.contains(keyword, true)
            }

        val coinMarketToRenderIds = coinMarketToRender.map { it.id }.toSet()

        coinMarketToRender.forEachIndexed { _, coinListEntry ->
            if (shouldBuildSeparator) {
                watchEntrySeparatorView {
                    id("separator_result_${coinListEntry.id}")
                }
            }
            if (!shouldBuildSeparator) {
                shouldBuildSeparator = true
            }

            watchEntryView {
                id(coinListEntry.id)
                symbol(coinListEntry.symbol)
                name(coinListEntry.name)
                val priceModel = WatchEntryView.PriceModel(
                    price = coinListEntry.currentPrice,
                    changePercent = coinListEntry.priceChangePercentage24h,
                    currency = currencyCode
                )
                price(priceModel)
                graph(null)
            }
        }

        val coinListToRender = coinList
            .filter { it.id !in watchListIds && it.id !in coinMarketToRenderIds }
            .filter {
                it.name.contains(keyword, true)
                        || it.symbol.contains(keyword, true)
            }
            .sortedWith(
                compareByDescending<CoinListEntry> {
                    max(calculateRatio(keyword, it.name), calculateRatio(keyword, it.symbol))
                }.thenByDescending {
                    coinMarketToRenderIds.contains(it.id)
                }
            )

        coinListToRender.forEachIndexed { _, coinListEntry ->
            if (shouldBuildSeparator) {
                watchEntrySeparatorView {
                    id("separator_result_${coinListEntry.id}")
                }
            }
            if (!shouldBuildSeparator) {
                shouldBuildSeparator = true
            }

            watchEntryView {
                id(coinListEntry.id)
                symbol(coinListEntry.symbol)
                name(coinListEntry.name)
                price(null)
                graph(null)
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

    private fun buildWatchList(state: WatchListState): BuildState {
        val userSetting = state.userSetting.invoke() ?: return BuildState.Next
        val watchList = state.watchList.invoke() ?: return BuildState.Next
        val currencyCode = userSetting.currencyCode

        if (state.keyword.isNotEmpty()) {
            watchHeaderView {
                id("header_watch_list")
                header(R.string.watch_list_header)
            }
        }

        watchList.forEachIndexed { index, coinId ->
            if (index != 0) {
                watchEntrySeparatorView {
                    id("watch_list_separator_$coinId")
                }
            }

            val coinDetail = state.watchEntryDetail[coinId]?.invoke()
            val coinMarket = state.watchEntryMarket[coinId]?.invoke()
            watchEntryView {
                id(coinId)
                symbol(coinDetail?.symbol)
                name(coinDetail?.name)
                val priceModel = if (coinDetail != null) {
                    WatchEntryView.PriceModel(
                        price = coinMarket?.prices?.lastOrNull()?.get(1)
                            ?: coinDetail.marketData.currentPrice[currencyCode]!!,
                        changePercent = coinDetail.marketData
                            .priceChangePercentage24hInCurrency[currencyCode]!!,
                        currency = currencyCode
                    )
                } else {
                    null
                }
                price(priceModel)
                graph(coinMarket?.prices?.filter { it.size == 2 })
            }
        }

        return BuildState.Next
    }


    @AssistedFactory
    interface Factory {
        fun create(viewModel: WatchListViewModel): WatchListController
    }
}