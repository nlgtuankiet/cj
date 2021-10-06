package com.rainyseason.cj.detail

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.detail.view.intervalSegmentedView
import com.rainyseason.cj.detail.view.namePriceChangeView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CoinDetailController @AssistedInject constructor(
    @Assisted val viewModel: CoinDetailViewModel,
    private val numberFormater: NumberFormater,
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        buildNamePrice(state)
        buildIntervalegment(state)
    }

    private fun buildIntervalegment(state: CoinDetailState) {

        intervalSegmentedView {
            id("interval")
            interval(state.selectedInterval)
            onIntervalClickListener {
                viewModel.onIntervalClick(it)
            }
        }

    }

    private fun buildNamePrice(state: CoinDetailState): BuildState {
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Next
        val userSetting = state.userSetting.invoke() ?: return BuildState.Next

        namePriceChangeView {
            id("name_price_change")
            name(coinDetail.name)
            price(
                numberFormater.formatAmount(
                    amount = coinDetail.marketData.currentPrice[userSetting.currencyCode]!!,
                    currencyCode = userSetting.currencyCode,
                    roundToMillion = true,
                    numberOfDecimal = 2,
                    hideOnLargeAmount = true,
                    showCurrencySymbol = true,
                    showThousandsSeparator = true
                )
            )
            changePercent(
                numberFormater.formatPercent(
                    amount = coinDetail.marketData.priceChangePercentage24hInCurrency[userSetting.currencyCode]!!,
                    locate = SUPPORTED_CURRENCY[userSetting.currencyCode]!!.locale,
                    numberOfDecimals = 2,
                )
            )
        }


        return BuildState.Next
    }


    @AssistedFactory
    interface Factory {
        fun create(viewModel: CoinDetailViewModel): CoinDetailController
    }
}