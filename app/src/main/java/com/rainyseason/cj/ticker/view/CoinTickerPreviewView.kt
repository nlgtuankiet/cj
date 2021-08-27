package com.rainyseason.cj.ticker.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.R
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.inflateAndAdd
import com.rainyseason.cj.ticker.TickerWidgetRenderParams

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class CoinTickerPreviewView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val remoteView: RemoteViews
    private val renderer = coreComponent.tickerWidgetRender

    init {
        inflateAndAdd(R.layout.coin_ticker_preview_view)
        remoteView = LocalRemoteViews(
            context,
            findViewById(R.id.preview_container),
            R.layout.widget_coin_ticker
        )
    }

    @ModelProp
    fun setRenderParams(params: TickerWidgetRenderParams?) {
        params?.let {
            renderer.render(remoteView, it)
        }
    }
}