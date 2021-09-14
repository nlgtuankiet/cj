package com.rainyseason.cj.ticker.list.view

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelView
import com.airbnb.epoxy.TextProp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflateAndAdd

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_MATCH_HEIGHT)
class CoinTickerListRetryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {

    init {
        inflateAndAdd(R.layout.view_coin_ticker_list_retry)
    }

    private val reasonText: TextView = findViewById(R.id.reason_text)
    private val retryButton: Button = findViewById(R.id.retry_button)


    @TextProp
    fun setReason(value: CharSequence?) {
        reasonText.text = value
    }

    @TextProp
    fun setButtonText(value: CharSequence?) {
        retryButton.text = value
    }

    @CallbackProp
    fun setOnRetryClickListener(l: OnClickListener?) {
        retryButton.setOnClickListener(l)
    }


}