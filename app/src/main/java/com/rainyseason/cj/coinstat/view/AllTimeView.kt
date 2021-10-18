package com.rainyseason.cj.coinstat.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.CoinStatAllTimeViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class AllTimeView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    private val binding = CoinStatAllTimeViewBinding
        .inflate(inflater, this, true)

    @ModelProp
    fun setMax(value: Int) {
        binding.progress.max = value
    }

    @ModelProp
    fun setCurrent(value: Int) {
        binding.progress.setProgressCompat(value, true)
    }

    @TextProp
    fun setStartPrice(value: CharSequence) {
        binding.startPrice.text = value
    }

    @TextProp
    fun setEndPrice(value: CharSequence) {
        binding.endPrice.text = value
    }

    @TextProp
    fun setStartDate(value: CharSequence) {
        binding.startDate.text = value
    }

    @TextProp
    fun setEndDate(value: CharSequence) {
        binding.endDate.text = value
    }
}
