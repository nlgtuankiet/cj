package com.rainyseason.cj.common

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.DiffResult
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.IdUtils
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.bumptech.glide.Glide
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.databinding.OnboardFeatureLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class OnBoardParam(
    val coroutineScope: CoroutineScope,
    val focusId: String,
    val epoxyRecyclerView: EpoxyRecyclerView,
    val controller: EpoxyController,
    val parentView: View,
    val blockerView: View,
    val onboardContainer: ViewGroup,
    val onBoardTitleRes: Int,
    val onBoardDescriptionRes: Int,
    val onBoardImageRes: Int? = null,
    val onDoneListener: () -> Unit = {}
)

suspend fun KeyValueStore.isOnboardDone(featureName: String): Boolean {
    return getBoolean("onboard_done_$featureName") == true
}

suspend fun KeyValueStore.setOnboardDone(featureName: String) {
    setBoolean("onboard_done_$featureName", true)
}

suspend fun OnBoardParam.show() {
    showOnBoard(this)
}

private suspend fun showOnBoard(
    params: OnBoardParam
) {
    val recyclerView = params.epoxyRecyclerView
    val controller = params.controller
    val adapter = controller.adapter

    val parentView = params.parentView
    val onboardContainer = params.onboardContainer
    val blockerView = params.blockerView
    blockerView.setOnClickListener { }
    val idHash = IdUtils.hashString64Bit(params.focusId)

    suspend fun findItemView(): View? {
        Timber.d("find index")
        var index = -1
        fun findIndex(models: List<EpoxyModel<*>>) {
            index = models.indexOfFirst {
                it.id() == idHash
            }
        }
        findIndex(adapter.copyOfModels)
        if (index == -1) {
            suspendCancellableCoroutine<Unit> { cont ->
                val listener = object : OnModelBuildFinishedListener {
                    override fun onModelBuildFinished(result: DiffResult) {
                        findIndex(adapter.copyOfModels)
                        if (index != -1) {
                            cont.resume(Unit)
                            controller.removeModelBuildListener(this)
                        }
                    }
                }
                cont.invokeOnCancellation {
                    controller.removeModelBuildListener(listener)
                }
                controller.addModelBuildListener(listener)
            }
        }
        recyclerView.awaitScrollState(RecyclerView.SCROLL_STATE_IDLE)
        recyclerView.smoothScrollToPosition(index)
        recyclerView.awaitScrollState(RecyclerView.SCROLL_STATE_IDLE)
        return recyclerView.findViewHolderForAdapterPosition(index)?.itemView
    }

    fun cleanUp() {
        blockerView.isGone = true
        onboardContainer.isGone = true
        onboardContainer.removeAllViews()
    }

    var itemView: View? = null
    while (itemView == null) {
        itemView = findItemView()
    }

    val bottomAmount = recyclerView.height - itemView.bottom
    Timber.d("bottomAmount $bottomAmount")
    // TODO do not show onboard ui if bottom space less than 56dp (user can not press "ok" button)
    if (bottomAmount < recyclerView.context.dpToPx(56)) {
        // no space left to show onboard ui, scroll up a bit
        if (recyclerView.canScrollVertically(1)) {
            val scrollAmount = itemView.top
            recyclerView.smoothScrollBy(0, scrollAmount)
            recyclerView.awaitScrollState(RecyclerView.SCROLL_STATE_IDLE)
        }
    }

    fun findMarginTop(targetView: View, parentView: View): Int {
        var margin = 0
        var currentChild: View? = targetView
        var currentParent: View? = targetView.parent as? View
        while (currentParent != parentView) {
            margin += currentChild?.top ?: 0
            currentChild = currentParent
            currentParent = currentChild?.parent as? View
        }
        margin += currentChild?.top ?: 0
        return margin
    }

    blockerView.isGone = false
    onboardContainer.isGone = false

    val focusHeight = itemView.measuredHeight
    val focusMarginTop = findMarginTop(itemView, parentView)
    val focusBinding = OnboardFeatureLayoutBinding.inflate(
        recyclerView.inflater,
        onboardContainer,
        true
    )
    focusBinding.title.setText(params.onBoardTitleRes)
    focusBinding.description.setText(params.onBoardDescriptionRes)
    if (params.onBoardImageRes != null) {
        Glide.with(focusBinding.image)
            .load(params.onBoardImageRes)
            .into(focusBinding.image)
    }
    focusBinding.focusPoint.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        height = focusHeight
        updateMargins(top = focusMarginTop)
    }
    onboardContainer.showWithAnimation()
    focusBinding.ok.setOnClickListener {
        params.coroutineScope.launch {
            onboardContainer.hideWithAnimation()
            cleanUp()
            params.onDoneListener.invoke()
        }
    }
    Timber.d("done")
}
