package com.rainyseason.cj.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.bumptech.glide.Glide
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class SettingTitleSummaryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    init {
        inflateAndAdd(R.layout.setting_title_summary_view)
    }

    private val titleView = findViewById<TextView>(R.id.title)
    private val summaryView = findViewById<TextView>(R.id.summary)
    private val imagePrimary = findViewById<ImageView>(R.id.image_primary)
    private val imageSecondary = findViewById<ImageView>(R.id.image_secondary)

    @TextProp
    fun setTitle(value: CharSequence) {
        titleView.text = value
    }

    @TextProp
    fun setSummary(value: CharSequence) {
        summaryView.text = value
    }

    @ModelProp(ModelProp.Option.IgnoreRequireHashCode)
    @JvmOverloads
    fun setImagePrimary(model: Any? = null) {
        bindModelToImage(model, imagePrimary)
    }

    @ModelProp(ModelProp.Option.IgnoreRequireHashCode)
    @JvmOverloads
    fun setImageSecondary(model: Any? = null) {
        bindModelToImage(model, imageSecondary)
    }

    private fun bindModelToImage(model: Any?, imageView: ImageView) {
        imageView.isGone = model == null
        if (model == null) {
            Glide.with(imageView).clear(imageView)
        } else {
            Glide.with(imageView)
                .load(model)
                .into(imageView)
        }
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}
