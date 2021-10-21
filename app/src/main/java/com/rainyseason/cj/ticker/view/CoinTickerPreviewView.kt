package com.rainyseason.cj.ticker.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RemoteViews
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.R
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.inflateAndAdd
import com.rainyseason.cj.ticker.CoinTickerConfig
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
        val iconLayouts = listOf(
            CoinTickerConfig.Layout.ICON_SMALL
        )
        if (params.config.layout in iconLayouts && params.data.iconUrl.isNotBlank()) {
            progressBar.isGone = false
            GlideApp.with(this)
                .asBitmap()
                .override(context.dpToPx(48), context.dpToPx(48))
                .load(params.data.iconUrl)
                .into(object : CustomViewTarget<View, Bitmap>(this) {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        progressBar.isGone = true
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        progressBar.isGone = true
                        renderer.render(remoteView!!, params, resource)
                    }

                    override fun onResourceCleared(placeholder: Drawable?) {
                    }
                })
        } else {
            renderer.render(remoteView!!, params)
        }
    }
}
