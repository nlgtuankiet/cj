package com.rainyseason.cj.watch.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.ViewWatchEntryBinding
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import kotlin.math.abs

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class WatchEntryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : FrameLayout(context, attributeSet) {
    private val numberFormater = coreComponent.numberFormater
    private val binding: ViewWatchEntryBinding = ViewWatchEntryBinding
        .inflate(inflater, this, true)
    private val graphRenderer = coreComponent.graphRenderer

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

    @ModelProp
    fun setGraph(value: List<List<Double>>?) {
        binding.graph.isGone = value.isNullOrEmpty()

        if (!value.isNullOrEmpty()) {
            val start = value.first()[1]
            val end = value.last()[1]
            val startIndex = value.size * 6 / 7
            binding.graph.doOnPreDraw {
                val bitmap = graphRenderer.createGraphBitmap(
                    context = context,
                    width = binding.graph.measuredWidth.toFloat(),
                    height = binding.graph.measuredHeight.toFloat(),
                    isPositive = end > start || DebugFlag.POSITIVE_WIDGET.isEnable,
                    data = value
                )
                binding.graph.setImageBitmap(bitmap)
            }
        }
    }

    @ModelProp
    fun setPrice(model: PriceModel?) {
        if (model == null) {
            binding.price.text = ""
            binding.price.isVisible = false

            binding.changePercent.text = ""
            binding.changePercent.isVisible = false
            return
        }

        binding.price.isVisible = true
        binding.changePercent.isVisible = true

        val currency = model.currency
        val locale = SUPPORTED_CURRENCY[currency]!!.locale
        binding.price.text = numberFormater.formatAmount(
            amount = model.price,
            currencyCode = currency,
            roundToMillion = true,
            numberOfDecimal = 2,
            hideOnLargeAmount = true,
            showCurrencySymbol = true,
            showThousandsSeparator = true,
        )
        val changePercent = model.changePercent
        if (changePercent != null) {
            binding.changePercent.text = numberFormater.formatPercent(
                amount = abs(changePercent),
                locate = locale,
                numberOfDecimals = 1
            )
            binding.changePercent.setBackgroundResource(
                if (changePercent > 0) {
                    R.drawable.watch_change_background_green
                } else {
                    R.drawable.watch_change_background_red
                }
            )
        } else {
            binding.changePercent.text = "--"
            binding.changePercent.setBackgroundResource(R.drawable.watch_change_background_none)
        }
    }

    @CallbackProp
    override fun setOnLongClickListener(l: OnLongClickListener?) {
        super.setOnLongClickListener(l)
    }

    @CallbackProp
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
    }

    data class PriceModel(
        val price: Double,
        val changePercent: Double?,
        val currency: String,
    )
}
