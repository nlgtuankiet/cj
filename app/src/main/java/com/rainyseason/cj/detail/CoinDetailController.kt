package com.rainyseason.cj.detail

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.detail.view.graphView
import com.rainyseason.cj.detail.view.intervalSegmentedView
import com.rainyseason.cj.detail.view.namePriceChangeView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

class CoinDetailController @AssistedInject constructor(
    @Assisted val viewModel: CoinDetailViewModel,
    private val numberFormater: NumberFormater,
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        buildNamePrice(state)
        buildIntervalegment(state)
        buildGraph(state)
    }

    private fun buildGraph(state: CoinDetailState) {

        /**
         *                 TimeInterval.I_24H to 1,
        TimeInterval.I_30D to 30,
        TimeInterval.I_1Y to 365,
         */
        val responseInterval = when (state.selectedInterval) {
            TimeInterval.I_1H -> TimeInterval.I_24H
            TimeInterval.I_24H -> TimeInterval.I_24H
            TimeInterval.I_7D -> TimeInterval.I_30D
            TimeInterval.I_30D -> TimeInterval.I_30D
            TimeInterval.I_90D -> TimeInterval.I_1Y
            TimeInterval.I_1Y -> TimeInterval.I_1Y
            TimeInterval.I_ALL -> TimeInterval.I_1Y
        }

        val priceGraph = state.marketChartResponse[responseInterval]?.invoke()
            ?.prices?.filter { it.size == 2 }
            ?: return

        if (priceGraph.isEmpty()) {
            return
        }

        val currentTime = System.currentTimeMillis()
        val graphData = when (state.selectedInterval) {
            TimeInterval.I_1H -> priceGraph.filter { it[0] > currentTime - TimeUnit.HOURS.toMillis(1) }
            TimeInterval.I_24H -> priceGraph
            TimeInterval.I_7D -> priceGraph.filter { it[0] > currentTime - TimeUnit.DAYS.toMillis(7) }
            TimeInterval.I_30D -> priceGraph
            TimeInterval.I_90D -> priceGraph.filter {
                it[0] > currentTime - TimeUnit.DAYS.toMillis(90)
            }
            TimeInterval.I_1Y -> priceGraph
            TimeInterval.I_ALL -> priceGraph
        }

        graphView {
            id("graph")
            graph(graphData)
        }
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