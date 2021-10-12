package com.rainyseason.cj.widget.watch

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.WatchPreviewViewBinding
import java.io.File
import java.io.FileOutputStream

class WatchPreviewView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    private val binding = WatchPreviewViewBinding.inflate(inflater, this, true)
    private var remoteView: RemoteViews? = null
    private val renderer = coreComponent.tickerWidgetRender
    private val container = binding.previewContainer
    private val mainContainer = binding.container
    private val progressBar = binding.progressBar
    private val captureButton = binding.capture

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

    fun setRenderParams(params: WatchRenderParams?) {
        if (params != null) {
            mainContainer.doOnPreDraw {
                // coreComponent.traceManager.endTrace(CoinTickerPreviewTTI(params.config.widgetId))
            }
        }
        container.removeAllViews()
        progressBar.isGone = params != null
        if (params == null) {
            return
        }
        // val layout = renderer.selectLayout(params.config)
        // remoteView = LocalRemoteViews(
        //     context,
        //     container,
        //     layout
        // )
        // val widgetSize = renderer.getWidgetSize(params.config)
        // container.updateLayoutParams<MarginLayoutParams> {
        //     height = widgetSize.height
        //     width = widgetSize.width
        // }
        // mainContainer.updateLayoutParams<MarginLayoutParams> {
        //     height = widgetSize.height + context.dpToPx(12 * 2)
        // }
        // renderer.render(remoteView!!, params)
    }
}