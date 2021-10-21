package com.rainyseason.cj.coinselect.view

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
class CoinView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    init {
        inflateAndAdd(R.layout.coin_select_coin_view)
    }

    private val nameTextView = findViewById<TextView>(R.id.name)
    private val symbolTextView = findViewById<TextView>(R.id.symbol)

    @TextProp
    fun setName(value: CharSequence) {
        nameTextView.text = value
    }

    @TextProp
    fun setSymbol(value: CharSequence) {
        symbolTextView.text = value
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}
