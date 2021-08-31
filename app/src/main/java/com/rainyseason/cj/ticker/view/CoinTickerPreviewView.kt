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
    init {
        inflateAndAdd(R.layout.coin_ticker_preview_view)
    }

    private var remoteView: RemoteViews? = null
    private val renderer = coreComponent.tickerWidgetRender
    private val container = findViewById<FrameLayout>(R.id.preview_container)
    private var currentLayout: Int? = null

    @ModelProp
    fun setRenderParams(params: TickerWidgetRenderParams?) {
        params?.let {
            val layout = renderer.selectLayout(params.config)
            if (currentLayout != layout) {
                currentLayout = layout
                container.removeAllViews()
                remoteView = LocalRemoteViews(
                    context,
                    container,
                    layout
                )
            }
            remoteView?.let { view ->
                renderer.render(view, params)
            }
        }
    }
}