package com.rainyseason.cj.common.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class SettingAdvanceView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    init {
        inflateAndAdd(R.layout.setting_title_summary_view)
    }

    private val titleView = findViewById<TextView>(R.id.title)
    private val summaryView = findViewById<TextView>(R.id.summary)

    @TextProp
    fun setTitle(value: CharSequence) {
        titleView.text = value
    }

    @TextProp
    fun setSummary(value: CharSequence) {
        summaryView.text = value
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}
