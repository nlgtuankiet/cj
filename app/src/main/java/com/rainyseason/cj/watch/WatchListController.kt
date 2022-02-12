package com.rainyseason.cj.watch

import android.view.Gravity
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.detail.CoinDetailArgs
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logClick
import com.rainyseason.cj.watch.view.WatchEntryView
import com.rainyseason.cj.watch.view.WatchEntryViewModelBuilder
import com.rainyseason.cj.watch.view.watchEditEntryView
import com.rainyseason.cj.watch.view.watchEntrySeparatorView
import com.rainyseason.cj.watch.view.watchEntryView
import com.rainyseason.cj.widget.watch.WatchDisplayEntryLoadParam
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class WatchListController @AssistedInject constructor(
    @Assisted val viewModel: WatchListViewModel,
    private val tracker: Tracker,
) : AsyncEpoxyController() {

    var touchHelper: ItemTouchHelper? = null

    override fun buildModels() {
        emptyView { id("holder") }
        val state = withState(viewModel) { it }

        buildWatchList(state)
        buildEditList(state)
    }

    private fun buildEditList(state: WatchListState): BuildState {
        if (!state.isInEditMode) {
            return BuildState.Next
        }

        val watchList = state.currentWatchlist ?: return BuildState.Next
        val currency = state.userSetting.invoke()?.currencyCode ?: return BuildState.Next

        watchList.coins.forEachIndexed { _, coin: Coin ->
            val loadParam = WatchDisplayEntryLoadParam(
                coin,
                currency,
                changeInterval = TimeInterval.I_24H,
            )
            val displayData = state.watchDisplayData[loadParam]?.invoke()
            watchEditEntryView {
                id("watch_entry_edit_${coin.hashCode()}_${state.selectedWatchlistId}")
                coin(coin)
                symbol(displayData?.symbol)
                name(displayData?.name)
                onDeleteClickListener { _ ->
                    tracker.logClick(
                        screenName = WatchListFragment.SCREEN_NAME,
                        target = "entry",
                        params = mapOf(
                            "action" to "delete",
                        ) + coin.getTrackingParams()
                    )
                    viewModel.onRemoveClick(coin, state.selectedWatchlistId)
                }
                onHandleTouch { model, _, _, _ ->
                    adapter.boundViewHolders.getHolderForModel(model)?.let {
                        touchHelper?.startDrag(it)
                    }
                }
            }
        }

        return BuildState.Next
    }

    private fun showPopup(view: View, coin: Coin, watchlistId: String) {
        val state = withState(viewModel) { it }
        if (state.isInEditMode) {
            return
        }

        view.createPopupMenuCenterEnd().apply {
            inflate(R.menu.watch_list_watch_item)
            setOnMenuItemClickListener { menu ->
                tracker.logClick(
                    screenName = WatchListFragment.SCREEN_NAME,
                    target = "entry_popup",
                    params = mapOf(
                        "action" to "remove"
                    )
                )
                viewModel.onRemoveClick(coin, watchlistId)
                true
            }
        }.show()
    }

    private fun View.createPopupMenuCenterEnd(): PopupMenu {
        return PopupMenu(context, this, Gravity.CENTER_VERTICAL or Gravity.END)
    }

    private fun WatchEntryViewModelBuilder.setupOnClick(
        coin: Coin,
    ) {
        onClickListener { view ->
            view.requestFocus()
            tracker.logClick(
                screenName = WatchListFragment.SCREEN_NAME,
                target = "entry",
                params = mapOf(
                    "action" to "open_coin_detail",
                ) + coin.getTrackingParams()
            )
            view.findNavController()
                .navigate(R.id.detail_screen, CoinDetailArgs(coin).asArgs())
        }
    }

    private fun buildWatchList(state: WatchListState): BuildState {
        if (state.isInEditMode) {
            return BuildState.Next
        }

        val watchList = state.currentWatchlist ?: return BuildState.Next
        val currency = state.userSetting.invoke()?.currencyCode ?: return BuildState.Next

        watchList.coins.forEachIndexed { index, coin: Coin ->
            val loadParam = WatchDisplayEntryLoadParam(
                coin,
                currency,
                changeInterval = TimeInterval.I_24H,
            )
            val displayData = state.watchDisplayData[loadParam]?.invoke()
            if (index != 0) {
                watchEntrySeparatorView {
                    id("watch_entry_separator_${coin.hashCode()}_${state.selectedWatchlistId}")
                }
            }
            watchEntryView {
                id("watch_entry_${coin.hashCode()}_${state.selectedWatchlistId}")
                symbol(displayData?.symbol ?: "")
                name(displayData?.name ?: "")
                val priceModel = if (displayData != null) {
                    WatchEntryView.PriceModel(
                        price = displayData.price,
                        changePercent = displayData.changePercent,
                        currency = currency,
                    )
                } else {
                    null
                }
                price(priceModel)
                graph(displayData?.graph ?: emptyList())
                setupOnClick(coin)
                onLongClickListener { view ->
                    showPopup(view, coin, state.selectedWatchlistId)
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
