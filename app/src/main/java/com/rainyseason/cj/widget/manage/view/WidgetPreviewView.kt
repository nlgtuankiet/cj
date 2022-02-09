package com.rainyseason.cj.widget.manage.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.AttributeSet
import android.util.Size
import android.widget.FrameLayout
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.model.WidgetRenderParams
import com.rainyseason.cj.databinding.ManageWidgetPreviewViewBinding

@ModelView(autoLayout = ModelView.Size.WRAP_WIDTH_WRAP_HEIGHT)
class WidgetPreviewView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

    data class Param(
        val renderParam: WidgetRenderParams?,
        val ratio: Size,
    )

    private val binding = ManageWidgetPreviewViewBinding.inflate(inflater, this, true)
    private val sizeMultiplier = context.dpToPx(75)

    @ModelProp
    fun setIsLoading(isLoading: Boolean) {
        binding.progressBar.isGone = !isLoading
        if (isLoading) {
            binding.content.setBackgroundResource(R.drawable.manage_widget_preview_background)
        } else {
            binding.content.background = null
        }
    }

    @ModelProp
    fun setRenderParam(param: Param) {
        binding.imageView.isInvisible = param.renderParam == null
        val ratio = param.ratio
        binding.imageView.updateLayoutParams<MarginLayoutParams> {
            width = sizeMultiplier * ratio.width
            height = sizeMultiplier * ratio.height
        }
        if (param.renderParam == null) {
            return
        }
        GlideApp.with(binding.imageView)
            .load(param.renderParam)
            .into(binding.imageView)
    }

    fun getPreviewBitmap(): Bitmap? {
        val bitmapDrawable = binding.imageView.drawable as? BitmapDrawable
        return bitmapDrawable?.bitmap
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        setOnLongClickListener(
            l?.let {
                OnLongClickListener {
                    l.onClick(it)
                    true
                }
            }
        )
    }
}
