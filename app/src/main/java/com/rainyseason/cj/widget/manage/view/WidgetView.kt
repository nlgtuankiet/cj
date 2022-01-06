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
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.model.WidgetRenderParams
import com.rainyseason.cj.databinding.ManageWidgetWidgetViewBinding

data class WidgetParam(
    val ratio: Size,
    val widgetRenderParam: WidgetRenderParams
)

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WidgetView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

    private val binding = ManageWidgetWidgetViewBinding.inflate(inflater, this, true)
    private val dimenMultiplier = context.dpToPx(75)

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

    @ModelProp
    fun setWidgetParams(params: WidgetParam) {
        val max = 3
        val ratioMax = params.ratio.width.coerceAtLeast(params.ratio.height)
        val scaleFactor = (1.0 * 3 / ratioMax).coerceAtMost(1.0)

        binding.widgetContainer.updateLayoutParams<MarginLayoutParams> {
            width = (dimenMultiplier * params.ratio.width * scaleFactor).toInt()
            height = (dimenMultiplier * params.ratio.height * scaleFactor).toInt()
        }
        GlideApp.with(binding.widgetContainer)
            .load(params.widgetRenderParam)
            .into(binding.widgetContainer)
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
