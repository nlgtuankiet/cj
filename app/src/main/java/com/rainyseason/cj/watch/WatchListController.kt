package com.rainyseason.cj.watch

import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.detail.CoinDetailArgs
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logClick
import com.rainyseason.cj.watch.view.WatchEntryView
import com.rainyseason.cj.watch.view.WatchEntryViewModelBuilder
import com.rainyseason.cj.watch.view.watchEditEntryView
import com.rainyseason.cj.watch.view.watchEntrySeparatorView
import com.rainyseason.cj.watch.view.watchEntryView
import com.rainyseason.cj.watch.view.watchHeaderView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlin.math.max

class WatchListController @AssistedInject constructor(
    @Assisted val viewModel: WatchListViewModel,
    private val tracker: Tracker,
) : AsyncEpoxyController() {
    override fun buildModels() {
        emptyView { id("holder") }
        val state = withState(viewModel) { it }

        buildWatchList(state)
        buildSearchResult(state)
        buildEditList(state)
    }

    private fun buildEditList(state: WatchListState): BuildState {
        if (!state.isInEditMode) {
            return BuildState.Next
        }

        val watchList = state.watchList.invoke() ?: return BuildState.Next

        watchList.forEachIndexed { _, coinId ->
            val coinDetail = state.watchEntryDetail[coinId]?.invoke()
            watchEditEntryView {
                id(coinId)
                coinId(coinId)
                symbol(coinDetail?.symbol)
                name(coinDetail?.name)
                onDeleteClickListener { _ ->
                    tracker.logClick(
                        screenName = WatchListFragment.SCREEN_NAME,
                        target = "entry",
                        params = mapOf(
                            "action" to "delete",
                            "coin_id" to coinId
                        )
                    )
                    viewModel.onRemoveClick(coinId)
                }
            }
        }

        return BuildState.Next
    }

    private fun buildSearchResult(state: WatchListState): BuildState {
        if (state.isInEditMode) {
            return BuildState.Next
        }
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

        var shouldBuildSeparator = false

        val coinMarketToRender = coinMarket
            .filter { it.id !in watchListIds }
            .filter {
                it.name.contains(keyword, true) ||
                    it.symbol.contains(keyword, true)
            }

        val coinMarketIds = coinMarket.map { it.id }.toSet()
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
                coinId(coinListEntry.id)
                symbol(coinListEntry.symbol)
                name(coinListEntry.name)
                val priceModel = WatchEntryView.PriceModel(
                    price = coinListEntry.currentPrice,
                    changePercent = coinListEntry.priceChangePercentage24h,
                    currency = currencyCode
                )
                price(priceModel)
                graph(null)
                setupOnClick(coinListEntry.id, coinListEntry.symbol)
                onLongClickListener { view ->
                    showPopup(view, coinListEntry.id)
                    true
                }
            }
        }

        val coinListToRender = coinList
            .filter { it.id !in watchListIds && it.id !in coinMarketToRenderIds }
            .filter {
                it.name.contains(keyword, true) ||
                    it.symbol.contains(keyword, true)
            }
            .sortedWith(
                compareByDescending<CoinListEntry> {
                    max(calculateRatio(keyword, it.name), calculateRatio(keyword, it.symbol))
                }.thenByDescending {
                    coinMarketIds.contains(it.id)
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
                coinId(coinListEntry.id)
                symbol(coinListEntry.symbol)
                name(coinListEntry.name)
                price(null)
                graph(null)
                setupOnClick(coinListEntry.id, coinListEntry.symbol)
                onLongClickListener { view ->
                    showPopup(view, coinListEntry.id)
                    true
                }
            }
        }

        return BuildState.Next
    }

    private fun showPopup(view: View, coinId: String) {
        val state = withState(viewModel) { it }
        if (state.isInEditMode) {
            return
        }
        val inWatchList = coinId in state.watchList.invoke().orEmpty()
        val action = if (inWatchList) {
            "remove"
        } else {
            "add"
        }

        view.createPopupMenuCenterEnd().apply {
            if (inWatchList) {
                inflate(R.menu.watch_list_watch_item)
            } else {
                inflate(R.menu.watch_list_search_result_item)
            }
            setOnMenuItemClickListener { menu ->
                tracker.logClick(
                    screenName = WatchListFragment.SCREEN_NAME,
                    target = "entry_popup",
                    params = mapOf(
                        "action" to action
                    )
                )
                when (menu.itemId) {
                    R.id.add -> viewModel.onAddClick(coinId)
                    R.id.remove -> viewModel.onRemoveClick(coinId)
                }
                true
            }
        }.show()
    }

    private fun View.createPopupMenuCenterEnd(): PopupMenu {
        return PopupMenu(context, this, Gravity.CENTER_VERTICAL or Gravity.END)
    }

    private fun calculateRatio(keyword: String, value: String): Double {
        if (value.contains(keyword, true)) {
            return keyword.length.toDouble() / value.length
        }
        return 0.0
    }

    private fun WatchEntryViewModelBuilder.setupOnClick(
        coinId: String,
        symbol: String? = null,
    ) {
        onClickListener { view ->
            tracker.logClick(
                screenName = WatchListFragment.SCREEN_NAME,
                target = "entry",
                params = mapOf(
                    "action" to "open_coin_detail",
                    "coin_id" to coinId
                )
            )
            view.findNavController().navigate(R.id.detail, CoinDetailArgs(coinId, symbol).asArgs())
        }
    }

    private fun buildWatchList(state: WatchListState): BuildState {
        if (state.isInEditMode) {
            return BuildState.Next
        }
        val userSetting = state.userSetting.invoke() ?: return BuildState.Next
        val watchList = state.watchList.invoke() ?: return BuildState.Next
        val currencyCode = userSetting.currencyCode

        val keyword = state.keyword

        if (state.keyword.isNotEmpty()) {
            watchHeaderView {
                id("header_watch_list")
                header(R.string.watch_list_header)
            }
        }

        val filteredWatchList = if (keyword.isNotBlank()) {
            watchList
                .filter { symbol ->
                    val detail = state.watchEntryDetail[symbol]?.invoke()
                    if (detail == null) {
                        return@filter true
                    } else {
                        detail.name.contains(keyword, true) ||
                            detail.symbol.contains(keyword, true)
                    }

                }
        } else {
            watchList
        }

        filteredWatchList.forEachIndexed { index, coinId ->
            if (index != 0) {
                watchEntrySeparatorView {
                    id("watch_list_separator_$coinId")
                }
            }

            val coinDetail = state.watchEntryDetail[coinId]?.invoke()
            val coinMarket = state.watchEntryMarket[coinId]?.invoke()
            watchEntryView {
                id(coinId)
                coinId(coinId)
                symbol(coinDetail?.symbol)
                name(coinDetail?.name)
                val priceModel = if (coinDetail != null) {
                    WatchEntryView.PriceModel(
                        price = coinMarket?.prices?.lastOrNull()?.get(1)
                            ?: coinDetail.marketData.currentPrice[currencyCode]!!,
                        changePercent = coinDetail.marketData
                            .priceChangePercentage24hInCurrency[currencyCode],
                        currency = currencyCode
                    )
                } else {
                    null
                }
                price(priceModel)
                graph(coinMarket?.prices?.filter { it.size == 2 })
                setupOnClick(coinId, coinDetail?.symbol)
                onLongClickListener { view ->
                    showPopup(view, coinId)
                    true
                }
            }
        }

        return BuildState.Next
    }

    @AssistedFactory
    interface Factory {
        fun create(viewModel: WatchListViewModel): WatchListController
    }
}
