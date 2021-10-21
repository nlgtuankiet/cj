package com.rainyseason.cj.coinselect.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class HeaderView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

    init {
        inflateAndAdd(R.layout.coin_select_header_view)
    }

    private val content: TextView = findViewById(R.id.content)

    @TextProp
    fun setContent(value: CharSequence) {
        content.text = value
    }
}
