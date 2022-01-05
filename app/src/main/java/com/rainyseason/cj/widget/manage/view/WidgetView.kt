package com.rainyseason.cj.widget.manage.view

import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ManageWidgetWidgetViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WidgetView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

    private val binding = ManageWidgetWidgetViewBinding.inflate(inflater, this, true)
    private val dimenMultiplier = context.dpToPx(75)

    @ModelProp
    fun setWidgetDimens(size: Size) {
        binding.widgetContainer.updateLayoutParams<MarginLayoutParams> {
            width = dimenMultiplier * size.width
            height = dimenMultiplier * size.height
        }
    }

    @ModelProp
    fun setIsRefreshing(isRefreshing: Boolean) {
        binding.progressBar.isGone = !isRefreshing
    }

    @TextProp
    fun setTitle(charSequence: CharSequence) {
        binding.title.text = charSequence
    }

    @TextProp
    fun setSubtitle(charSequence: CharSequence) {
        binding.subtitle.text = charSequence
    }

    @CallbackProp
    fun setOnRefreshClickListener(l: OnClickListener?) {
        binding.refresh.setOnClickListener(l)
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}