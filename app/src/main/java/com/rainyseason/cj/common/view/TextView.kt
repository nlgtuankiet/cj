package com.rainyseason.cj.common.view

import android.content.Context
import android.util.AttributeSet
import androidx.core.view.updatePadding
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.google.android.material.textview.MaterialTextView
import com.rainyseason.cj.common.dpToPx

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class TextView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : MaterialTextView(context, attributeSet) {

    @ModelProp
    override fun setTextColor(color: Int) {
        super.setTextColor(color)
    }

    @ModelProp
    @JvmOverloads
    fun setAlignment(textAlignment: Int = TEXT_ALIGNMENT_TEXT_START) {
        super.setTextAlignment(textAlignment)
    }

    @TextProp
    fun setContent(charSequence: CharSequence) {
        text = charSequence
    }

    @ModelProp
    @JvmOverloads
    fun setPaddingVertical(value: Int = 0) {
        val px = context.dpToPx(value)
        updatePadding(top = px, bottom = px)
    }

    @ModelProp
    @JvmOverloads
    fun setPaddingHorizontal(value: Int = 0) {
        val px = context.dpToPx(value)
        updatePadding(left = px, right = px)
    }
}
