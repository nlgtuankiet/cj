package com.rainyseason.cj.common.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.dpToPx

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class VerticalSpacerView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : View(context, attributeSet) {

    @ModelProp
    fun setHeight(dp: Int) {
        updateLayoutParams<ViewGroup.MarginLayoutParams> { height = context.dpToPx(dp) }
    }
}
