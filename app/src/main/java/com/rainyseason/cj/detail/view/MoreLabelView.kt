package com.rainyseason.cj.detail.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.CoinDetailMoreLabelViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class MoreLabelView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val binding = CoinDetailMoreLabelViewBinding
        .inflate(inflater, this, true)

    @TextProp
    fun setTitle(value: CharSequence) {
        binding.titleTextView.text = value
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}