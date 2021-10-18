package com.rainyseason.cj.detail.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.databinding.CoinDetailStatSummaryViewBinding

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class StatSummaryView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : FrameLayout(context, attributeSet) {
    private val binding = CoinDetailStatSummaryViewBinding
        .inflate(inflater, this, true)

    @ModelProp
    fun setMarketCap(value: String) {
        binding.marketCapValue.text = value
    }

    @ModelProp
    fun setCirculatingSupply(value: String) {
        binding.circulatingSupplyValue.text = value
    }

    @ModelProp
    fun setTotalSupply(value: String) {
        binding.totalSupplyValue.text = value
    }

    @ModelProp
    fun setAllTimeHigh(value: String) {
        binding.athValue.text = value
    }

    @ModelProp
    fun setVolume24h(value: String) {
        binding.volume24hValue.text = value
    }

    @ModelProp
    fun setMaxSupply(value: String) {
        binding.maxSupplyValue.text = value
    }

    @ModelProp
    fun setRank(value: String) {
        binding.rankValue.text = value
    }

    // suppose to be market cap % but we don't have that data yet
    @ModelProp
    fun setHashingAlgorithm(value: String) {
        binding.hashValue.text = value
    }
}
