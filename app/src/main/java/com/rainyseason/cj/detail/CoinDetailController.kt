package com.rainyseason.cj.detail

import androidx.navigation.findNavController
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.coinstat.CoinStatArgs
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.view.horizontalSeparatorView
import com.rainyseason.cj.detail.about.CoinDetailAboutArgs
import com.rainyseason.cj.detail.view.aboutView
import com.rainyseason.cj.detail.view.graphView
import com.rainyseason.cj.detail.view.intervalSegmentedView
import com.rainyseason.cj.detail.view.lowHighView
import com.rainyseason.cj.detail.view.moreLabelView
import com.rainyseason.cj.detail.view.namePriceChangeView
import com.rainyseason.cj.detail.view.statSummaryView
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logClick
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatterBuilder
import kotlin.math.abs

class CoinDetailController @AssistedInject constructor(
    @Assisted val viewModel: CoinDetailViewModel,
    @Assisted val args: CoinDetailArgs,
    private val numberFormatter: NumberFormater,
    private val tracker: Tracker,
) : AsyncEpoxyController() {

    private val builders = listOf(
        ::buildNamePrice,
        ::buildIntervalSegment,
        ::buildGraph,
        ::buildStatLabel,
        ::buildLowHigh,
        ::buildStatSummary,
        ::buildAbout,
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

    private fun buildAbout(state: CoinDetailState): BuildState {
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Stop
        val rawDescription = coinDetail.description?.get("en") ?: return BuildState.Next
        val description = rawDescription.replace("\n", "<br>")
        buildSeparator("about_label_separator")
        moreLabelView {
            id("about_label")
            title("About ${coinDetail.name}")
            onClickListener { view ->
                val args = CoinDetailAboutArgs(
                    coinName = coinDetail.name,
                    content = description
                )
                view.findNavController().navigate(
                    R.id.detail_about_screen,
                    args.asArgs()
                )
            }
        }
        buildSeparator("about_separator")
        aboutView {
            id("about")
            content(description)
            onClickListener { view ->
                val args = CoinDetailAboutArgs(
                    coinName = coinDetail.name,
                    content = description
                )
                view.findNavController().navigate(
                    R.id.detail_about_screen,
                    args.asArgs()
                )
            }
        }

        return BuildState.Next
    }

    private fun buildSeparator(id: String) {
        horizontalSeparatorView {
            id(id)
            margin(16)
        }
    }

    private fun buildStatLabel(state: CoinDetailState): BuildState {
        if (!BuildConfig.DEBUG) {
            return BuildState.Next
        }

        buildSeparator("stat_more_separator")

        moreLabelView {
            id("stat_more")
            title("Statistics")
            onClickListener { view ->
                val args = CoinStatArgs(
                    coinId = args.coinId,
                    symbol = state.coinDetailResponse.invoke()?.symbol
                ).asArgs()
                view.findNavController()
                    .navigate(R.id.coin_stat_screen, args)
            }
        }

        return BuildState.Next
    }

    private fun buildStatSummary(state: CoinDetailState): BuildState {
        val userSetting = state.userSetting.invoke() ?: return BuildState.Stop
        val currencyCode = userSetting.currencyCode
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Stop

        statSummaryView {
            id("stat_summary")

            val marketCapValue = numberFormatter.formatAmount(
                amount = coinDetail.marketData.marketCap[currencyCode]!!,
                currencyCode = currencyCode,
            )
            marketCap(marketCapValue)

            val circulatingSupply = coinDetail.marketData.circulatingSupply
            if (circulatingSupply == null) {
                circulatingSupply("--")
            } else {
                val circulatingSupplyValue = numberFormatter.formatAmount(
                    amount = coinDetail.marketData.circulatingSupply,
                    currencyCode = currencyCode,
                    showCurrencySymbol = false
                )
                circulatingSupply(circulatingSupplyValue)
            }

            val totalSupply = coinDetail.marketData.totalSupply
            if (totalSupply == null) {
                totalSupply("-")
            } else {
                val totalSupplyValue = numberFormatter.formatAmount(
                    amount = coinDetail.marketData.totalSupply,
                    currencyCode = currencyCode,
                    showCurrencySymbol = false
                )
                totalSupply(totalSupplyValue)
            }

            val ath = coinDetail.marketData.ath?.get(currencyCode)
            if (ath == null) {
                allTimeHigh("--")
            } else {
                val allTimeHighValue = numberFormatter.formatAmount(
                    amount = ath,
                    currencyCode = currencyCode,
                )
                allTimeHigh(allTimeHighValue)
            }

            // val volumn24hValue =

            val marketResponse24h = state.marketChartResponse[TimeInterval.I_24H]?.invoke()
            if (marketResponse24h == null) {
                volume24h("--")
            } else {
                val value = marketResponse24h.totalVolumes.last()[1]
                val volume24hValue = numberFormatter.formatAmount(
                    amount = value,
                    currencyCode = currencyCode,
                )
                volume24h(volume24hValue)
            }

            val maxSupply = coinDetail.marketData.maxSupply
            if (maxSupply == null) {
                maxSupply("-")
            } else {
                val maxSupplyValue = numberFormatter.formatAmount(
                    amount = maxSupply,
                    currencyCode = currencyCode,
                    showCurrencySymbol = false
                )
                maxSupply(maxSupplyValue)
            }

            val rank = coinDetail.marketCapRank
            if (rank == null) {
                rank("-")
            } else {
                rank("#$rank")
            }

            val hashingAlgorithm = coinDetail.hashingAlgorithm
            if (hashingAlgorithm == null) {
                hashingAlgorithm("--")
            } else {
                hashingAlgorithm(hashingAlgorithm)
            }
        }

        return BuildState.Next
    }

    private fun buildLowHigh(state: CoinDetailState): BuildState {
        val userSetting = state.userSetting.invoke() ?: return BuildState.Stop
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Stop

        buildSeparator("low_high_separator")

        val lowHigh = state.lowHighPrice
        val lowPrice = lowHigh?.first?.let {
            numberFormatter.formatAmount(
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
            numberFormatter.formatAmount(
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
                tracker.logClick(
                    screenName = CoinDetailFragment.SCREEN_NAME,
                    target = "low_high_interval",
                    mapOf("interval" to interval.id)
                )
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
                    numberFormatter.formatAmount(
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

    private fun buildIntervalSegment(state: CoinDetailState): BuildState {

        intervalSegmentedView {
            id("interval")
            interval(state.selectedInterval)
            onIntervalClickListener {
                viewModel.onIntervalClick(it)
                tracker.logClick(
                    screenName = CoinDetailFragment.SCREEN_NAME,
                    target = "time_interval",
                    mapOf("interval" to it.id)
                )
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
        val actualChangePercent = if (
            changePercent != null &&
            changePercent < 0 &&
            DebugFlag.POSITIVE_WIDGET.isEnable
        ) {
            abs(changePercent)
        } else {
            changePercent
        }
        val changePercentText = if (actualChangePercent != null) {
            numberFormatter.formatPercent(
                amount = actualChangePercent,
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
                numberFormatter.formatAmount(
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
        fun create(
            viewModel: CoinDetailViewModel,
            args: CoinDetailArgs,
        ): CoinDetailController
    }
}
