package com.rainyseason.cj.coinstat

import android.content.Context
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.coinstat.view.entryView
import com.rainyseason.cj.coinstat.view.titleView
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.view.horizontalSeparatorView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class CoinStatController @AssistedInject constructor(
    @Assisted private val viewModel: CoinStatViewModel,
    @Assisted private val context: Context,
    private val numberFormatter: NumberFormater
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        buildPriceGroup(state)
    }

    private fun buildPriceGroup(state: CoinStatState) {
        val coinDetail = state.coinDetail.invoke() ?: return
        val userSetting = state.userSetting.invoke() ?: return
        val currencyCode = userSetting.currencyCode
        val marketChart24h = state.marketChart[TimeInterval.I_24H]?.invoke()

        titleView {
            id("price_title")
            title("Price")
        }

        val priceAmount = coinDetail.marketData.currentPrice[currencyCode]!!
        val price = numberFormatter.formatAmount(
            amount = priceAmount,
            currencyCode = currencyCode,
            numberOfDecimal = if (priceAmount > 0) 2 else 4,
        )
        buildSeparator("current_price_separator")
        entryView {
            id("current_price")
            title("Current Price")
            value(price)
        }
    }

    private fun buildSeparator(id: String) {
        horizontalSeparatorView {
            id(id)
            margin(12)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: CoinStatViewModel,
            context: Context,
        ): CoinStatController
    }
}