package com.rainyseason.cj.detail.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.transition.TransitionManager
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.databinding.ViewDetailLowHighBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class LowHighView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    private val binding = ViewDetailLowHighBinding.inflate(inflater, this, true)
    private val intervalToView = mapOf(
        TimeInterval.I_24H to binding.i24h,
        TimeInterval.I_30D to binding.i30d,
        TimeInterval.I_1Y to binding.i1y,
    )
    private val movingCursor = binding.movingCursor
    private val segmentContainer = binding.segmentContainer

    init {
        intervalToView.forEach { (interval, view) ->
            view.setOnClickListener {
                onIntervalClickListener?.invoke(interval)
            }
        }
    }

    @ModelProp
    fun setInterval(interval: TimeInterval) {
        val focusView = intervalToView[interval]!!
        val currentSet = ConstraintSet()
        currentSet.clone(segmentContainer)
        listOf(
            ConstraintSet.START,
            ConstraintSet.TOP,
            ConstraintSet.END,
            ConstraintSet.BOTTOM,
        ).forEach { side ->
            currentSet.connect(movingCursor.id, side, focusView.id, side)
        }
        currentSet.applyTo(segmentContainer)
        TransitionManager.beginDelayedTransition(segmentContainer, IntervalSegmentedView.trasition)
    }

    @set:CallbackProp
    var onIntervalClickListener: ((TimeInterval) -> Unit)? = null

    @ModelProp
    fun setMax(value: Int) {
        binding.progress.max = value
    }

    @ModelProp
    fun setCurrent(value: Int) {
        binding.progress.setProgressCompat(value, true)
    }

    @ModelProp
    fun setStartPrice(value: String) {
        binding.startPrice.text = value
    }

    @ModelProp
    fun setEndPrice(value: String) {
        binding.endPrice.text = value
    }
}
