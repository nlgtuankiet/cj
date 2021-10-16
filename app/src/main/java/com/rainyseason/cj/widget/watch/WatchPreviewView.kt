package com.rainyseason.cj.widget.watch

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Size
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.WatchPreviewViewBinding
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

class WatchPreviewView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    private val binding = WatchPreviewViewBinding.inflate(inflater, this, true)
    private var remoteView: RemoteViews? = null
    private val renderer = coreComponent.watchWidgetRenderer
    private val previewContainer = binding.previewContainer
    private val mainContainer = binding.container
    private val progressBar = binding.progressBar
    private val captureButton = binding.capture

    init {
        captureButton.isVisible = BuildConfig.DEBUG && DebugFlag.SHOW_CAPTURE_BUTTON.isEnable
        captureButton.setOnClickListener {
            @Suppress("DEPRECATION")
            previewContainer.isDrawingCacheEnabled = true
            @Suppress("DEPRECATION")
            val b: Bitmap = previewContainer.drawingCache
            b.compress(
                Bitmap.CompressFormat.PNG,
                100,
                FileOutputStream(File(context.cacheDir, "capture.png"))
            )
        }
    }

    fun setRenderParams(params: WatchWidgetRenderParams?) {
        if (params != null) {
            mainContainer.doOnPreDraw {
                // coreComponent.traceManager.endTrace(CoinTickerPreviewTTI(params.config.widgetId))
            }
        }
        previewContainer.removeAllViews()
        progressBar.isGone = params != null
        if (params == null) {
            return
        }

        remoteView = LocalRemoteViews(
            context,
            previewContainer,
            params.config.layout.layout,
        )

        // val widgetSize = renderer.getWidgetSize(params.config)
        // container.updateLayoutParams<MarginLayoutParams> {
        //     height = widgetSize.height
        //     width = widgetSize.width
        // }
        // mainContainer.updateLayoutParams<MarginLayoutParams> {
        //     height = widgetSize.height + context.dpToPx(12 * 2)
        // }

        renderer.render(remoteView!!, params)
    }

    fun setOnScaleClickListener(l: OnClickListener?) {
        binding.scale.setOnClickListener(l)
    }

    private val dp48 = context.dpToPx(48)

    fun setScale(
        scale: Double,
        config: WatchConfig,
    ) {
        val widgetSize = renderer.getWidgetSize(config)
        val scaledSize = Size(
            (widgetSize.width * scale).toInt(),
            (widgetSize.height * scale).toInt(),
        )
        previewContainer.updateLayoutParams<MarginLayoutParams> {
            height = scaledSize.height
            width = scaledSize.width
        }

        binding.progressBar.scaleX = scale.toFloat()
        binding.progressBar.scaleY = scale.toFloat()

        val scaled = widgetSize != scaledSize

        mainContainer.updateLayoutParams<MarginLayoutParams> {
            height = scaledSize.height + if (scaled) dp48 else dp48 * 2
        }

        binding.scale.isSelected = scaled
        Timber.d("beginDelayedTransition")
    }
}
