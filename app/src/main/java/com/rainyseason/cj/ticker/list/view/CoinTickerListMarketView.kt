package com.rainyseason.cj.ticker.list.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class CoinTickerListMarketView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    init {
        inflateAndAdd(R.layout.setting_market_view)
    }

    private val icon: ImageView = findViewById(R.id.icon)
    private val name: TextView = findViewById(R.id.name)
    private val symbol: TextView = findViewById(R.id.symbol)

    @ModelProp
    fun setIconUrl(value: String) {
        GlideApp.with(icon)
            .load(value)
            .into(icon)
    }

    @TextProp
    fun setName(value: CharSequence) {
        name.text = value
    }

    @TextProp
    fun setSymbol(value: CharSequence) {
        symbol.text = value
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}
