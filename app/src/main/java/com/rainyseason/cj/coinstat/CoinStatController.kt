package com.rainyseason.cj.coinstat

import android.content.Context
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.coinstat.view.allTimeView
import com.rainyseason.cj.coinstat.view.entryView
import com.rainyseason.cj.coinstat.view.priceRangeView
import com.rainyseason.cj.coinstat.view.titleView
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.view.horizontalSeparatorView
import com.rainyseason.cj.data.locale
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatterBuilder
import kotlin.math.abs

class CoinStatController @AssistedInject constructor(
    @Assisted private val viewModel: CoinStatViewModel,
    @Assisted private val context: Context,
    private val numberFormatter: NumberFormater
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        buildPriceGroup(state)
    }

    private fun buildAllTime(state: CoinStatState): BuildState {
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Stop
        val userSetting = state.userSetting.invoke() ?: return BuildState.Stop
        val ath = coinDetail.marketData.ath?.get(userSetting.currencyCode)
        val athDate = coinDetail.marketData.athDate?.get(userSetting.currencyCode)
        val atl = coinDetail.marketData.atl?.get(userSetting.currencyCode)
        val atlDate = coinDetail.marketData.atlDate?.get(userSetting.currencyCode)
        val currentPrice = coinDetail.marketData.currentPrice[userSetting.currencyCode]!!


        buildSeparator("price_range_separator")
        allTimeView {
            id("all_time")

            if (atl != null) {
                startPrice(
                    numberFormatter.formatAmount(
                        amount = atl,
                        currencyCode = userSetting.currencyCode,
                        numberOfDecimal = if (atl > 0) 2 else 4,
                        hideOnLargeAmount = false,
                    )
                )
            } else {
                startPrice("--")
            }


            if (ath != null) {
                endPrice(
                    numberFormatter.formatAmount(
                        amount = ath,
                        currencyCode = userSetting.currencyCode,
                        numberOfDecimal = if (ath > 0) 2 else 4,
                        hideOnLargeAmount = false,
                    )
                )
            } else {
                endPrice("--")
            }


            if (ath != null && atl != null) {
                val percent = 100 * (currentPrice - atl) / (ath - atl)
                max(100)
                current(percent.toInt())
            } else {
                max(100)
                current(0)
            }

            val formatter = DateTimeFormatterBuilder()
                .appendPattern("d MMM YYYY")
                .toFormatter(userSetting.locale)
            if (atlDate != null) {
                val time = ZonedDateTime.parse(atlDate)
                startDate(formatter.format(time))
            } else {
                startDate("--")
            }

            if (athDate != null) {
                val time = ZonedDateTime.parse(athDate)
                endDate(formatter.format(time))
            } else {
                endDate("--")
            }
        }

        return BuildState.Next
    }

    private fun buildPriceRange(state: CoinStatState): BuildState {
        val userSetting = state.userSetting.invoke() ?: return BuildState.Stop
        val coinDetail = state.coinDetailResponse.invoke() ?: return BuildState.Stop

        buildSeparator("price_range_separator")

        val lowHigh = state.priceRange
        val lowPrice = lowHigh?.first?.let {
            numberFormatter.formatAmount(
                amount = lowHigh.first,
                currencyCode = userSetting.currencyCode,
                numberOfDecimal = if (lowHigh.first > 0) 2 else 4,
                hideOnLargeAmount = false,
            )
        } ?: ""
        val highPrice = lowHigh?.second?.let {
            numberFormatter.formatAmount(
                amount = lowHigh.second,
                currencyCode = userSetting.currencyCode,
                numberOfDecimal = if (lowHigh.second > 0) 2 else 4,
                hideOnLargeAmount = false,
            )
        } ?: ""

        priceRangeView {
            id("price_range")
            interval(state.selectedPriceRange)
            onIntervalClickListener { interval ->
                // TODO
                // tracker.logClick(
                //     screenName = CoinDetailFragment.SCREEN_NAME,
                //     target = "low_high_interval",
                //     mapOf("interval" to interval.id)
                // )
                viewModel.onSelectPriceRange(interval)
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

    private fun buildPriceChange24h(state: CoinStatState) {
        val coinDetail = state.coinDetailResponse.invoke() ?: return
        val userSetting = state.userSetting.invoke() ?: return
        val currencyCode = userSetting.currencyCode

        val priceChange24h = coinDetail.marketData.priceChange24hInCurrency[currencyCode]!!
        val priceChange24hContent = numberFormatter.formatAmount(
            amount = priceChange24h,
            currencyCode = currencyCode,
            numberOfDecimal = if (abs(priceChange24h) > 0) 2 else 4,
            hideOnLargeAmount = false,
        )

        buildSeparator("price_change_24h_separator")
        entryView {
            id("price_change_24h")
            title("Price Change")
            timeBadge("24h")
            value(priceChange24hContent)
        }
    }

    private fun buildCurrentPrice(state: CoinStatState) {
        val coinDetail = state.coinDetailResponse.invoke() ?: return
        val userSetting = state.userSetting.invoke() ?: return
        val currencyCode = userSetting.currencyCode

        val priceAmount = coinDetail.marketData.currentPrice[currencyCode]!!
        val priceContent = numberFormatter.formatAmount(
            amount = priceAmount,
            currencyCode = currencyCode,
            numberOfDecimal = if (priceAmount > 0) 2 else 4,
            hideOnLargeAmount = false,
        )

        val change24hPercent = coinDetail.marketData
            .priceChangePercentage24hInCurrency[currencyCode]!!
        val change24hPercentContent = numberFormatter.formatPercent(
            amount = change24hPercent,
            locate = userSetting.locale,
        )
        val change24hColor = if (change24hPercent > 0) {
            R.color.green_700
        } else {
            R.color.red_600
        }
        buildSeparator("current_price_separator")
        entryView {
            id("current_price")
            title("Current Price")
            value(buildSpannedString {
                append(priceContent)
                append("   ")

                color(context.getColorCompat(change24hColor)) {
                    append(change24hPercentContent)
                }
            })
        }
    }

    private fun buildPriceGroup(state: CoinStatState) {
        titleView {
            id("price_title")
            title("Price")
        }

        buildCurrentPrice(state)
        buildPriceChange24h(state)
        buildPriceRange(state)
        buildAllTime(state)
        // TODO ROI
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
