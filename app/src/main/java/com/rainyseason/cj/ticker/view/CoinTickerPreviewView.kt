package com.rainyseason.cj.ticker.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RemoteViews
import androidx.core.view.isGone
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.R
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.inflateAndAdd
import com.rainyseason.cj.ticker.TickerWidgetRenderParams
import timber.log.Timber

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
    private val progressBar: ProgressBar by lazy { findViewById(R.id.progress_bar) }

    // work around auto text size problem
    @ModelProp
    fun setRenderParams(params: TickerWidgetRenderParams?) {
        container.removeAllViews()
        progressBar.isGone = params != null
        if (params == null) {
            return
        }
        val layout = renderer.selectLayout(params.config)
        remoteView = LocalRemoteViews(
            context,
            container,
            layout
        )
        renderer.render(remoteView!!, params)
    }

    fun setRenderParamsOld(params: TickerWidgetRenderParams?) {
        Timber.d("setRenderParams: $params")
        progressBar.isGone = params != null
        if (params == null) {
            remoteView = null
            container.removeAllViews()
            return
        }
        val layout = renderer.selectLayout(params.config)
        if (currentLayout != layout) {
            currentLayout = layout
            remoteView = null
            container.removeAllViews()

        }
        if (remoteView == null) {
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