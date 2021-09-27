package com.rainyseason.cj.common.home

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT)
class DemoView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    init {
        inflateAndAdd(R.layout.view_home_image)
    }


    private val imageView: ImageView = findViewById(R.id.image)

    @ModelProp
    fun setImageRes(id: Int) {
        GlideApp.with(imageView)
            .load(id)
            .fitCenter()
            .into(imageView)
    }
}