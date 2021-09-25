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
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.inflateAndAdd
import jp.wasabeef.glide.transformations.ColorFilterTransformation

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class CoinTickerListHistoryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {

    init {
        inflateAndAdd(R.layout.view_setting_list_history_view)
    }

    private val icon: ImageView = findViewById(R.id.icon)
    private val name: TextView = findViewById(R.id.name)
    private val symbol: TextView = findViewById(R.id.symbol)
    private val cancel: ImageView = findViewById(R.id.cancel_button)

    @ModelProp
    fun setIconUrl(value: String?) {
        if (value == null) {
            GlideApp.with(icon)
                .load(R.drawable.ic_baseline_access_time_24)
                .transform(
                    MultiTransformation(
                        CenterCrop(),
                        ColorFilterTransformation(context.getColorCompat(R.color.gray_500)),
                    )
                )
                .into(icon)
        } else {
            GlideApp.with(icon)
                .load(value)
                .into(icon)
        }
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
    fun setOnCancelClickListener(l: OnClickListener?) {
        cancel.setOnClickListener(l)
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }
}