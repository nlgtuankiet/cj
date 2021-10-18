package com.rainyseason.cj.coinstat.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.CoinStatTitleViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class TitleView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val binding = CoinStatTitleViewBinding
        .inflate(inflater, this, true)

    @TextProp
    fun setTitle(value: CharSequence) {
        binding.title.text = value
    }
}
