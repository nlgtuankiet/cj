package com.rainyseason.cj.detail.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.isInvisible
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ViewDetailNamePriceChangeBinding


@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class NamePriceChangeView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    val binding = ViewDetailNamePriceChangeBinding.inflate(inflater, this, true)

    @ModelProp
    fun setName(value: String) {
        binding.name.text = value
    }

    @ModelProp
    fun setPrice(value: String) {
        binding.price.text = value
    }

    @ModelProp
    fun setChangePercent(value: String) {
        binding.changePercent.text = value
    }

    @ModelProp
    fun setChangePercentPositive(value: Boolean?) {
        val baclgroundRes = when (value) {
            true -> R.drawable.detail_change_green
            false -> R.drawable.detail_change_red
            null -> R.drawable.detail_change_unknown
        }
        binding.changePercent.setBackgroundResource(baclgroundRes)
    }

    @ModelProp
    fun setDate(value: String?) {
        binding.name.isInvisible = value != null
        binding.date.isInvisible = value == null
        binding.date.text = value
    }
}