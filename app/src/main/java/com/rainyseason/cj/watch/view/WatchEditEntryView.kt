package com.rainyseason.cj.watch.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isGone
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ViewWatchEditEntryBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WatchEditEntryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    private val binding: ViewWatchEditEntryBinding = ViewWatchEditEntryBinding
        .inflate(inflater, this, true)

    @set:ModelProp
    var coinId: String = ""

    @ModelProp
    fun setSymbol(value: String?) {
        binding.symbol.text = value ?: ""
        binding.symbolLoading.isGone = !value.isNullOrEmpty()
    }

    @ModelProp
    fun setName(value: String?) {
        binding.name.text = value ?: ""
        binding.nameLoading.isGone = !value.isNullOrEmpty()
    }

    @CallbackProp
    fun setOnDeleteClickListener(l: OnClickListener?) {
        binding.deleteButton.setOnClickListener(l)
    }

    data class PriceModel(
        val price: Double,
        val changePercent: Double?,
        val currency: String,
    )
}
