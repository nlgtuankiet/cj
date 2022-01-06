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
import com.bumptech.glide.request.target.Target
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ManageWidgetWidgetViewBinding
import com.rainyseason.cj.ticker.CoinTickerRenderParams

data class WidgetRenderParam(
    val ratio: Size,
    val coinTickerRenderParams: CoinTickerRenderParams? = null
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

    private var target: Target<*>? = null

    @ModelProp
    fun setTickerWidgetParams(params: WidgetRenderParam) {
        GlideApp.with(binding.widgetContainer).clear(target)
        binding.widgetContainer.updateLayoutParams<MarginLayoutParams> {
            width = dimenMultiplier * params.ratio.width
            height = dimenMultiplier * params.ratio.height
        }
        val tickerParams = params.coinTickerRenderParams
        if (tickerParams != null) {
            target = GlideApp.with(binding.widgetContainer)
                .load(tickerParams)
                .into(binding.widgetContainer)
        }
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