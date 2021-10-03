package com.rainyseason.cj.watch

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.withState
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.watch.view.WatchEntryView
import com.rainyseason.cj.watch.view.watchAddEntry
import com.rainyseason.cj.watch.view.watchEntrySeparatorView
import com.rainyseason.cj.watch.view.watchEntryView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

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
        val coinList = state.coinList.invoke() ?: return BuildState.Next
        val coinMarket = state.coinMarket.invoke() ?: return BuildState.Next


        coinMarket.forEachIndexed { index, coinListEntry ->
            watchAddEntry {
                id("add_${coinListEntry.id}")
                name(coinListEntry.name)
                symbol(coinListEntry.symbol)
                val isLoading = state.addTasks[coinListEntry.id] is Loading
                if (isLoading) {
                    isAdded(null)
                } else {
                    isAdded(watchList.contains(coinListEntry.id))
                }
                onAddClickListener { _ ->
                    viewModel.onAddClick(coinListEntry.id)
                }
            }
        }

        return BuildState.Next
    }

    private fun buildWatchList(state: WatchListState): BuildState {
        val userSetting = state.userSetting.invoke() ?: return BuildState.Next
        val watchList = state.watchList.invoke() ?: return BuildState.Next
        val currencyCode = userSetting.currencyCode

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