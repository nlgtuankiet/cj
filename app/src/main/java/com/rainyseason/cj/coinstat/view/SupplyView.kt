package com.rainyseason.cj.coinstat.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.CoinStatSupplyViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class SupplyView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    private val binding = CoinStatSupplyViewBinding
        .inflate(inflater, this, true)

    @ModelProp
    fun setMax(value: Int) {
        binding.progress.max = value
    }

    @ModelProp
    fun setCurrent(value: Int) {
        binding.progress.setProgressCompat(value, false)
    }

    @TextProp
    fun setCirculatingSupply(value: CharSequence) {
        binding.circulatingSupplyValue.text = value
    }

    @TextProp
    fun setMaxSupply(value: CharSequence) {
        binding.maxSupplyValue.text = value
    }

    @TextProp
    fun setTotalSupply(value: CharSequence) {
        binding.totalSupplyValue.text = value
    }
}
