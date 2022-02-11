package com.rainyseason.cj.watch

import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.EpoxyControllerAdapter
import com.airbnb.epoxy.EpoxyTouchHelper
import com.rainyseason.cj.R
import com.rainyseason.cj.common.hapticFeedback
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.databinding.FragmentWatchListBinding
import com.rainyseason.cj.tracking.logClick
import com.rainyseason.cj.watch.view.WatchEditEntryViewModel_
import timber.log.Timber

fun WatchListFragment.setUpEdit(
    binding: FragmentWatchListBinding,
    viewModel: WatchListViewModel,
    controller: WatchListController,
) {
    val editButton = binding.editButton
    val recyclerView = binding.contentRecyclerView
    val pullToRefreshLayout = binding.refreshLayout

    viewModel.onEach(WatchListState::isInEditMode) { isInEditMode ->
        pullToRefreshLayout.isEnabled = !isInEditMode
    }

    var moveFrom: Coin? = null
    var moveTo: Int? = null
    var mapping: Map<Int, Coin>? = null

    fun applyMapping() {
        val newMapping = mutableMapOf<Int, Coin>()
        val adapter = recyclerView.adapter as EpoxyControllerAdapter
        adapter.copyOfModels.forEachIndexed { index, epoxyModel ->
            val castModel = epoxyModel as? WatchEditEntryViewModel_
            if (castModel != null) {
                newMapping[index] = castModel.coin()
            }
        }
        mapping = newMapping
    }

    fun mayBeMove() {
        val fromCoin = moveFrom ?: return
        val currentMapping = mapping ?: return
        val toIndex = moveTo ?: return
        val toCoin = currentMapping[toIndex] ?: return
        Timber.d("move")
        viewModel.drag(fromCoin, toCoin)
        tracker.logClick(
            screenName = WatchListFragment.SCREEN_NAME,
            target = "entry",
            params = mapOf(
                "action" to "drag",
            )
        )
    }

    val helper: ItemTouchHelper = EpoxyTouchHelper.initDragging(controller)
        .withRecyclerView(recyclerView)
        .forVerticalList()
        .withTarget(WatchEditEntryViewModel_::class.java)
        .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<WatchEditEntryViewModel_>() {
            override fun onDragStarted(
                model: WatchEditEntryViewModel_,
                itemView: View,
                adapterPosition: Int
            ) {
                applyMapping()
                moveFrom = mapping?.get(adapterPosition)
                itemView.hapticFeedback()
            }

            override fun onDragReleased(model: WatchEditEntryViewModel_, itemView: View) {
                moveFrom = null
                moveTo = null
                mapping = null
            }

            override fun onModelMoved(
                fromPosition: Int,
                toPosition: Int,
                modelBeingMoved: WatchEditEntryViewModel_,
                itemView: View
            ) {
                if (toPosition != moveTo) {
                    itemView.hapticFeedback()
                    moveTo = toPosition
                }
                mayBeMove()
            }
        })
    controller.touchHelper = helper

    viewModel.onEach(WatchListState::isInEditMode) { isInEditMode ->
        editButton.setImageResource(
            if (isInEditMode) R.drawable.ic_baseline_done_24 else R.drawable.ic_baseline_edit_24
        )
        editButton.setOnClickListener {
            tracker.logClick(
                screenName = WatchListFragment.SCREEN_NAME,
                target = "edit_button",
                params = mapOf("is_in_edit_mode" to isInEditMode)
            )
            viewModel.switchEditMode()
        }
        if (isInEditMode) {
            helper.attachToRecyclerView(recyclerView)
        } else {
            helper.attachToRecyclerView(null)
        }
    }
}
