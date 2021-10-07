package com.rainyseason.cj.detail

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.detail.view.graphView
import com.rainyseason.cj.detail.view.intervalSegmentedView
import com.rainyseason.cj.detail.view.namePriceChangeView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatterBuilder

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
        val graphData = state.graphData

        val userSetting = state.userSetting.invoke() ?: return
        graphView {
            id("graph")
            graph(graphData)
            startPrice(
                if (graphData.isEmpty()) {
                    ""
                } else {
                    numberFormater.formatAmount(
                        amount = graphData[0][1],
                        currencyCode = userSetting.currencyCode,
                        roundToMillion = true,
                        numberOfDecimal = 2,
                        hideOnLargeAmount = true,
                        showCurrencySymbol = true,
                        showThousandsSeparator = true
                    )
                }
            )
            onDataTouchListener { index ->
                viewModel.setDataTouchIndex(index)
            }
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
        val graphData = state.graphData

        val selectedData = if (state.selectedIndex != null) {
            graphData.getOrNull(state.selectedIndex)
        } else {
            null
        }
        val currencyInfo = SUPPORTED_CURRENCY[userSetting.currencyCode]!!

        val coinPrice = coinDetail.marketData.currentPrice[userSetting.currencyCode]!!
        val formater = DateTimeFormatterBuilder()
            .appendPattern("d MMM YYYY, HH:mm")
            .toFormatter(currencyInfo.locale)

        namePriceChangeView {
            id("name_price_change")
            name(coinDetail.name)
            price(
                numberFormater.formatAmount(
                    amount = selectedData?.get(1) ?: coinPrice,
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
            date(
                if (selectedData != null) {
                    val time = selectedData[0]
                    formater.format(Instant.ofEpochMilli(time.toLong())
                        .atZone(ZoneId.systemDefault()))
                } else {
                    null
                }
            )
        }


        return BuildState.Next
    }


    @AssistedFactory
    interface Factory {
        fun create(viewModel: CoinDetailViewModel): CoinDetailController
    }
}