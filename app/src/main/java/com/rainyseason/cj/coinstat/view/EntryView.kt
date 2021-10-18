package com.rainyseason.cj.coinstat.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isGone
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.CoinStatEntryViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class EntryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val binding = CoinStatEntryViewBinding
        .inflate(inflater, this, true)

    @TextProp
    fun setTitle(value: CharSequence) {
        binding.title.text = value
    }

    @TextProp
    @JvmOverloads
    fun setTimeBadge(value: CharSequence? = null) {
        binding.timeBadge.isGone = value.isNullOrBlank()
        binding.timeBadge.text = value
    }

    @TextProp
    fun setValue(value: CharSequence) {
        binding.valueTextView.text = value
    }

    @ModelProp
    @JvmOverloads
    fun setHasInfo(value: Boolean = false) {
        binding.timeBadge.isGone = !value
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}