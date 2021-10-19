package com.rainyseason.cj.detail.view

import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.text.HtmlCompat
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.CoinDetailAboutViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class AboutView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val binding = CoinDetailAboutViewBinding
        .inflate(inflater, this, true)

    @ModelProp
    fun setContent(value: String) {
        binding.content.text = HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_COMPACT)
        binding.content.movementMethod = LinkMovementMethod.getInstance()
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        binding.content.setOnClickListener(l)
    }
}
