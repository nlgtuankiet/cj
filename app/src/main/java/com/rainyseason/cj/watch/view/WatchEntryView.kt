package com.rainyseason.cj.watch.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ViewWatchEntryBinding


@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WatchEntryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    private val binding: ViewWatchEntryBinding = ViewWatchEntryBinding
        .inflate(inflater, this, true)

    @ModelProp
    fun setSymbol(value: String) {
        binding.symbol.text = value
    }

    @ModelProp
    fun setName(value: String) {
        binding.name.text = value
    }
}