package com.rainyseason.cj.widget.manage.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.FrameLayout
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ManageWidgetPreviewContainerBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WidgetPreviewContainer @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val binding = ManageWidgetPreviewContainerBinding.inflate(
        inflater, this, true
    )

    init {
        binding.carousel.setSnap(Gravity.START)
        binding.carousel.itemAnimator = null
    }

    @ModelProp
    fun setModels(models: List<EpoxyModel<*>>) {
        binding.carousel.setModels(models)
    }
}
