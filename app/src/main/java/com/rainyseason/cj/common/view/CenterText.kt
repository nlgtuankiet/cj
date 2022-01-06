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
class CenterText @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : MaterialTextView(context, attributeSet) {

    init {
        textAlignment = TEXT_ALIGNMENT_CENTER
    }

    @ModelProp
    override fun setTextColor(color: Int) {
        super.setTextColor(color)
    }

    @TextProp
    fun setContent(charSequence: CharSequence) {
        text = charSequence
    }

    @ModelProp
    fun setPaddingVertical(value: Int) {
        val px = context.dpToPx(value)
        updatePadding(top = px, bottom = px)
    }
}
