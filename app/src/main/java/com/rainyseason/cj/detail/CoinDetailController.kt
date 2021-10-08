package com.rainyseason.cj.detail

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.detail.view.graphView
import com.rainyseason.cj.detail.view.intervalSegmentedView
import com.rainyseason.cj.detail.view.lowHighView
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

    private val builders = listOf(
        ::buildNamePrice,
        ::buildIntervalegment,
        ::buildGraph,
        ::buildLowHigh,
    )

    override fun buildModels() {
        val state = withState(viewModel) { it }

        builders.forEach {
            val buildResult = it.invoke(state)
            if (buildResult == BuildState.Stop) {
                return
            }
        }
    }

    private fun buildLowHigh(state: CoinDetailState): BuildState {
        val userSetting = state.userSetting.invoke() ?: return BuildState.Stop
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Stop
        val lowHigh = state.lowHighPrice
        val lowPrice = lowHigh?.first?.let {
            numberFormater.formatAmount(
                amount = lowHigh.first,
                currencyCode = userSetting.currencyCode,
                roundToMillion = true,
                numberOfDecimal = 4,
                hideOnLargeAmount = true,
                showCurrencySymbol = true,
                showThousandsSeparator = true
            )
        } ?: ""
        val highPrice = lowHigh?.second?.let {
            numberFormater.formatAmount(
                amount = lowHigh.second,
                currencyCode = userSetting.currencyCode,
                roundToMillion = true,
                numberOfDecimal = 4,
                hideOnLargeAmount = true,
                showCurrencySymbol = true,
                showThousandsSeparator = true
            )
        } ?: ""

        lowHighView {
            id("low_high")
            interval(state.selectedLowHighInterval)
            onIntervalClickListener { interval ->
                viewModel.onSelectLowHigh(interval)
            }
            val currentPrice = coinDetail.marketData.currentPrice[userSetting.currencyCode]!!
            val maxPrice = lowHigh?.second
            if (maxPrice == null) {
                current(0)
            } else {
                current((100 * currentPrice / maxPrice).toInt())
            }
            max(100)
            startPrice(lowPrice)
            endPrice(highPrice)
        }
        return BuildState.Next
    }

    private fun buildGraph(state: CoinDetailState): BuildState {
        val graphData = state.graphData

        val userSetting = state.userSetting.invoke() ?: return BuildState.Stop
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
                        numberOfDecimal = 4,
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
        return BuildState.Next
    }

    private fun buildIntervalegment(state: CoinDetailState): BuildState {

        intervalSegmentedView {
            id("interval")
            interval(state.selectedInterval)
            onIntervalClickListener {
                viewModel.onIntervalClick(it)
            }
        }

        return BuildState.Next
    }

    private fun buildNamePrice(state: CoinDetailState): BuildState {
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Stop
        val userSetting = state.userSetting.invoke() ?: return BuildState.Stop
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

        val changePercent = state.graphChangePercent
        val changePercentText = if (changePercent != null) {
            numberFormater.formatPercent(
                amount = changePercent,
                locate = SUPPORTED_CURRENCY[userSetting.currencyCode]!!.locale,
                numberOfDecimals = 2,
            )
        } else {
            "--"
        }

        namePriceChangeView {
            id("name_price_change")
            name(coinDetail.name)
            price(
                numberFormater.formatAmount(
                    amount = selectedData?.get(1) ?: coinPrice,
                    currencyCode = userSetting.currencyCode,
                    roundToMillion = true,
                    numberOfDecimal = 4,
                    hideOnLargeAmount = true,
                    showCurrencySymbol = true,
                    showThousandsSeparator = true
                )
            )
            changePercent(changePercentText)
            changePercentPositive(changePercent?.let { it > 0 })
            date(
                if (selectedData != null) {
                    val time = selectedData[0]
                    formater.format(
                        Instant.ofEpochMilli(time.toLong())
                            .atZone(ZoneId.systemDefault())
                    )
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
