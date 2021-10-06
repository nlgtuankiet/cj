package com.rainyseason.cj.watch.view

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WatchHeaderView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : ConstraintLayout(context, attributeSet) {

    init {
        inflateAndAdd(R.layout.view_watch_header_view)
    }

    private var header = findViewById<TextView>(R.id.header_text_view)

    @TextProp
    fun setHeader(value: CharSequence) {
        header.text = value
    }
}