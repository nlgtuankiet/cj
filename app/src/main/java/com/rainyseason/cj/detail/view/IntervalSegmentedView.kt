package com.rainyseason.cj.detail.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.hapticFeedback
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.databinding.ViewDetailIntervalSegmentViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class IntervalSegmentedView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    private val binding = ViewDetailIntervalSegmentViewBinding.inflate(inflater, this, true)
    private val intervalToView = mapOf(
        TimeInterval.I_1H to binding.i1h,
        TimeInterval.I_24H to binding.i24h,
        TimeInterval.I_7D to binding.i7d,
        TimeInterval.I_30D to binding.i30d,
        TimeInterval.I_90D to binding.i90d,
        TimeInterval.I_1Y to binding.i1y,
        TimeInterval.I_ALL to binding.iall,
    )
    private val movingCursor = binding.movingCursor
    private val container = binding.container

    init {
        intervalToView.forEach { (interval, view) ->
            view.setOnClickListener {
                onIntervalClickListener?.invoke(interval)
            }
        }
    }

    @ModelProp
    fun setInterval(interval: TimeInterval) {
        hapticFeedback()
        val focusView = intervalToView[interval]!!
        val currentSet = ConstraintSet()
        currentSet.clone(container)
        listOf(
            ConstraintSet.START,
            ConstraintSet.TOP,
            ConstraintSet.END,
            ConstraintSet.BOTTOM,
        ).forEach { side ->
            currentSet.connect(movingCursor.id, side, focusView.id, side)
        }
        currentSet.applyTo(container)
        TransitionManager.beginDelayedTransition(container, trasition)
    }

    @set:CallbackProp
    var onIntervalClickListener: ((TimeInterval) -> Unit)? = null

    companion object {
        val trasition = AutoTransition().apply {
            interpolator = FastOutSlowInInterpolator()
        }
    }
}
