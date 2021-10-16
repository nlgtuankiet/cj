package com.rainyseason.cj.watch

import android.view.View
import com.airbnb.epoxy.EpoxyControllerAdapter
import com.airbnb.epoxy.EpoxyTouchHelper
import com.rainyseason.cj.R
import com.rainyseason.cj.common.hapticFeedback
import com.rainyseason.cj.databinding.FragmentWatchListBinding
import com.rainyseason.cj.tracking.logClick
import com.rainyseason.cj.watch.view.WatchEditEntryViewModel_

fun WatchListFragment.setUpEdit(
    binding: FragmentWatchListBinding,
) {
    val editButton = binding.searchGroup.editButton
    val recyclerView = binding.contentRecyclerView

    var moveFrom: Int? = null
    var moveTo: Int? = null
    var mapping: Map<Int, String>? = null

    val helper = EpoxyTouchHelper.initDragging(controller)
        .withRecyclerView(recyclerView)
        .forVerticalList()
        .withTarget(WatchEditEntryViewModel_::class.java)
        .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<WatchEditEntryViewModel_>() {
            override fun onDragStarted(
                model: WatchEditEntryViewModel_,
                itemView: View,
                adapterPosition: Int
            ) {
                moveFrom = adapterPosition
                val newMapping = mutableMapOf<Int, String>()
                val adapter = recyclerView.adapter as EpoxyControllerAdapter
                val count = adapter.itemCount
                repeat(count) { position ->
                    val entryModel =
                        adapter.getModelAtPosition(position) as? WatchEditEntryViewModel_
                    if (entryModel != null) {
                        newMapping[position] = entryModel.coinId()
                    }
                }
                mapping = newMapping
                itemView.hapticFeedback()
            }

            override fun onDragReleased(model: WatchEditEntryViewModel_, itemView: View) {
                val currentMoveFrom = moveFrom
                val currentMoveTo = moveTo
                val currentMapping = mapping
                if (currentMoveFrom != null && currentMoveTo != null && currentMapping != null) {
                    val fromId = currentMapping[currentMoveFrom]
                    val toId = currentMapping[currentMoveTo]
                    if (fromId != null && toId != null) {
                        viewModel.drag(fromId, toId)
                        tracker.logClick(
                            screenName = WatchListFragment.SCREEN_NAME,
                            target = "entry",
                            params = mapOf(
                                "action" to "drag",
                            )
                        )
                    }
                }
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
            }
        })

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
            binding.searchGroup.cancelSearch.performClick()
            helper.attachToRecyclerView(recyclerView)
        } else {
            helper.attachToRecyclerView(null)
        }
    }
}