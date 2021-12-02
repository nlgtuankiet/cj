package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.model.Exchange
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.MarketChartResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Combine data with config to render UI
 *
 */
@JsonClass(generateAdapter = true)
data class CoinTickerDisplayData(
    @Json(name = "icon_url")
    val iconUrl: String,

    @Transient
    val iconBitmap: Bitmap? = null,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "price")
    val price: Double?, // null when coin is in preview

    @Json(name = "price_change_percent")
    val priceChangePercent: Double?,

    @Json(name = "market_cap")
    val marketCap: Double?,

    @Json(name = "market_cap_change_percent")
    val marketCapChangePercent: Double?,

    @Json(name = "price_graph")
    val priceGraph: List<List<Double>>? = null,

    @Json(name = "market_graph")
    val marketCapGraph: List<List<Double>>? = null,
) {

    data class LoadParam(
        val coinId: String,
        val exchange: Exchange,

    )

    fun getAmount(config: CoinTickerConfig): Double? {
        return if (price == null) {
            null
        } else {
            price * (config.amount ?: 1.0)
        }
    }

    companion object {
        /**
         * We get the price from market chart 24h because it better
         */
        fun create(
            config: CoinTickerConfig,
            coinDetail: CoinDetailResponse,
            marketChartResponse: Map<TimeInterval, MarketChartResponse?>,
            price: Double?,
        ): CoinTickerDisplayData {
            val currencyCode = config.currency
            val priceChangePercent = when (config.changeInterval) {
                TimeInterval.I_7D ->
                    coinDetail.marketData
                        .priceChangePercentage7dInCurrency[currencyCode]
                TimeInterval.I_14D ->
                    coinDetail.marketData
                        .priceChangePercentage14dInCurrency[currencyCode]
                TimeInterval.I_30D ->
                    coinDetail.marketData
                        .priceChangePercentage30dInCurrency[currencyCode]
                TimeInterval.I_1Y ->
                    coinDetail.marketData
                        .priceChangePercentage1yInCurrency[currencyCode]
                else ->
                    coinDetail.marketData
                        .priceChangePercentage24hInCurrency[currencyCode]
            }

            val marketCapChangePercent = marketChartResponse[config.changeInterval]
                ?.marketCaps
                ?.changePercent()
                ?.let { it * 100 }

            return CoinTickerDisplayData(
                iconUrl = coinDetail.image.large,
                symbol = coinDetail.symbol,
                name = coinDetail.name,
                price = price,
                priceChangePercent = priceChangePercent,
                marketCap = coinDetail.marketData.marketCap[currencyCode],
                marketCapChangePercent = marketCapChangePercent,
                priceGraph = marketChartResponse[config.changeInterval]?.prices,
                marketCapGraph = marketChartResponse[config.changeInterval]?.marketCaps,
            )
        }
    }
}
