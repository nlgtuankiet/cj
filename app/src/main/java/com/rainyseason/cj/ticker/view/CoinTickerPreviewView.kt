package com.rainyseason.cj.ticker.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RemoteViews
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.R
import com.rainyseason.cj.common.CoinTickerPreviewTTI
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.inflateAndAdd
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream


@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class CoinTickerPreviewView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    init {
        inflateAndAdd(R.layout.coin_ticker_preview_view)
    }

    private var remoteView: RemoteViews? = null
    private val renderer = coreComponent.tickerWidgetRender
    private val container = findViewById<FrameLayout>(R.id.preview_container)
    private val mainContainer = findViewById<ConstraintLayout>(R.id.container)
    private val progressBar: ProgressBar by lazy { findViewById(R.id.progress_bar) }
    private val captureButton = findViewById<ImageView>(R.id.capture)

    init {
        captureButton.isGone = !BuildConfig.DEBUG
        captureButton.setOnClickListener {
            @Suppress("DEPRECATION")
            container.isDrawingCacheEnabled = true
            @Suppress("DEPRECATION")
            val b: Bitmap = container.drawingCache
            b.compress(
                Bitmap.CompressFormat.PNG,
                100,
                FileOutputStream(File(context.cacheDir, "capture.png"))
            )
        }
    }

    // work around auto text size problem
    @ModelProp
    fun setRenderParams(params: CoinTickerRenderParams?) {
        Timber.d("render config ${params?.config}")
        if (params != null) {
            mainContainer.doOnPreDraw {
                coreComponent.traceManager.endTrace(CoinTickerPreviewTTI(params.config.widgetId))
            }
        }
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
        val widgetSize = renderer.getWidgetSize(params.config)
        container.updateLayoutParams<MarginLayoutParams> {
            height = widgetSize.height
            width = widgetSize.width
        }
        mainContainer.updateLayoutParams<MarginLayoutParams> {
            height = widgetSize.height + context.dpToPx(12 * 2)
        }
        renderer.render(remoteView!!, params)
    }

}