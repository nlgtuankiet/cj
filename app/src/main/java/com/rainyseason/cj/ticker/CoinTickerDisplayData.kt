package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.rainyseason.cj.common.changePercent
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
    val price: Double,

    @Json(name = "price_change_percent")
    val priceChangePercent: Double?,

    @Json(name = "market_cap")
    val marketCap: Double,

    @Json(name = "market_cap_change_percent")
    val marketCapChangePercent: Double?,

    @Json(name = "price_graph")
    val priceGraph: List<List<Double>>? = null,

    @Json(name = "market_graph")
    val marketCapGraph: List<List<Double>>? = null,
) {

    fun getChangePercent(config: CoinTickerConfig): Double? {
        return when (config.bottomContentType) {
            BottomContentType.PRICE -> priceChangePercent
            BottomContentType.MARKET_CAP -> marketCapChangePercent
            else -> error("Unknown ${config.bottomContentType}")
        }
    }

    fun getAmount(config: CoinTickerConfig): Double {
        return when (config.bottomContentType) {
            BottomContentType.PRICE -> price
            BottomContentType.MARKET_CAP -> marketCap
            else -> error("Unknown ${config.bottomContentType}")
        }
    }

    fun getGraphData(config: CoinTickerConfig): List<List<Double>>? {
        return when (config.bottomContentType) {
            BottomContentType.PRICE -> priceGraph
            BottomContentType.MARKET_CAP -> marketCapGraph
            else -> error("Unknown ${config.bottomContentType}")
        }
    }

    companion object {
        /**
         * We get the price from market chart 24h because it better
         */
        fun create(
            config: CoinTickerConfig,
            coinDetail: CoinDetailResponse,
            marketChartResponse: Map<String, MarketChartResponse?>,
            price: Double,
        ): CoinTickerDisplayData {
            val currencyCode = config.currency
            val priceChangePercent = when (config.changeInterval) {
                ChangeInterval._7D -> coinDetail.marketData.priceChangePercentage7dInCurrency[currencyCode]
                ChangeInterval._14D -> coinDetail.marketData.priceChangePercentage14dInCurrency[currencyCode]
                ChangeInterval._30D -> coinDetail.marketData.priceChangePercentage30dInCurrency[currencyCode]
                ChangeInterval._1Y -> coinDetail.marketData.priceChangePercentage1yInCurrency[currencyCode]
                else -> coinDetail.marketData.priceChangePercentage24hInCurrency[currencyCode]
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
                marketCap = coinDetail.marketData.marketCap[currencyCode]!!,
                marketCapChangePercent = marketCapChangePercent,
                priceGraph = marketChartResponse[config.changeInterval]?.prices,
                marketCapGraph = marketChartResponse[config.changeInterval]?.marketCaps,
            )
        }
    }
}