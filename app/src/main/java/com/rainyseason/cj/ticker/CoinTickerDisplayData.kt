package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.data.UserCurrency
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

    @Json(name = "amount")
    val amount: Double,

    @Json(name = "change_percent")
    val changePercent: Double?,

    @Json(name = "graph_data")
    val graphData: List<List<Double>>? = null,
) {

    companion object {
        fun create(
            config: CoinTickerConfig,
            userCurrency: UserCurrency,
            coinDetail: CoinDetailResponse,
            marketChartResponse: Map<String, MarketChartResponse?>,
        ): CoinTickerDisplayData {

            val priceChangePercent = when (config.changeInterval) {
                ChangeInterval._7D -> coinDetail.marketData.priceChangePercentage7dInCurrency[userCurrency.id]!!
                ChangeInterval._14D -> coinDetail.marketData.priceChangePercentage14dInCurrency[userCurrency.id]!!
                ChangeInterval._30D -> coinDetail.marketData.priceChangePercentage30dInCurrency[userCurrency.id]!!
                ChangeInterval._1Y -> coinDetail.marketData.priceChangePercentage1yInCurrency[userCurrency.id]!!
                else -> coinDetail.marketData.priceChangePercentage24hInCurrency[userCurrency.id]!!
            }

            val marketCapChangePercent = marketChartResponse[config.changeInterval]
                ?.marketCaps
                ?.changePercent()
                ?.let { it * 100 }

            val changePercent = when (config.bottomContentType) {
                BottomContentType.PRICE -> priceChangePercent
                BottomContentType.MARKET_CAP -> marketCapChangePercent
                else -> error("Unknown ${config.bottomContentType}")
            }

            val amount = when (config.bottomContentType) {
                BottomContentType.PRICE -> coinDetail.marketData.currentPrice[userCurrency.id]!!
                BottomContentType.MARKET_CAP -> coinDetail.marketData.marketCap[userCurrency.id]!!
                else -> error("Unknown ${config.bottomContentType}")
            }

            return CoinTickerDisplayData(
                iconUrl = coinDetail.image.large,
                symbol = coinDetail.symbol,
                name = coinDetail.name,
                amount = amount,
                changePercent = changePercent,
                graphData = when (config.bottomContentType) {
                    BottomContentType.PRICE -> marketChartResponse[config.changeInterval]?.prices
                    BottomContentType.MARKET_CAP -> marketChartResponse[config.changeInterval]?.marketCaps
                    else -> null
                }?.filter { it.size == 2 }
            )
        }
    }
}