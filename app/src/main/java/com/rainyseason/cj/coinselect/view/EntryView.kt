package com.rainyseason.cj.coinselect.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isGone
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class EntryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    init {
        inflateAndAdd(R.layout.coin_select_entry_view)
    }

    private val icon: ImageView = findViewById(R.id.icon)
    private val name: TextView = findViewById(R.id.name)
    private val symbol: TextView = findViewById(R.id.symbol)
    private val cancel: View = findViewById(R.id.cancel_button)

    @ModelProp
    fun setIconUrl(value: String) {
        GlideApp.with(icon)
            .load(value)
            .into(icon)
    }

    @TextProp
    fun setTitle(value: CharSequence) {
        name.text = value
    }

    @TextProp
    fun setSubTitle(value: CharSequence) {
        symbol.text = value
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }

    @CallbackProp
    fun setOnClearClickListener(l: OnClickListener?) {
        cancel.setOnClickListener(l)
        cancel.isGone = l == null
    }
}
