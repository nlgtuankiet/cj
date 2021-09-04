package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.rainyseason.cj.data.UserCurrency
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Combine data with config to render UI
 *
 */
@JsonClass(generateAdapter = true)
data class TickerWidgetDisplayData(
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

    @Json(name = "price_change_percent_24h")
    val priceChangePercent24h: Double,

    @Json(name = "price_change_percent_7d")
    val priceChangePercent7d: Double,

    @Json(name = "price_change_percent_14d")
    val priceChangePercent14d: Double,

    @Json(name = "price_change_percent_30d")
    val priceChangePercent30d: Double,

    @Json(name = "price_change_percent_60d")
    val priceChangePercent60d: Double,

    @Json(name = "price_change_percent_1y")
    val priceChangePercent1y: Double,

    @Json(name = "market_cap")
    val marketCap: Double,

    @Json(name = "market_cap_change_percentage_24h")
    val marketCapChangePercent24h: Double,
) {
    companion object {
        fun create(
            userCurrency: UserCurrency,
            coinDetail: CoinDetailResponse
        ): TickerWidgetDisplayData {
            return TickerWidgetDisplayData(
                iconUrl = coinDetail.image.large,
                symbol = coinDetail.symbol,
                name = coinDetail.name,
                price = coinDetail.marketData.currentPrice[userCurrency.id]!!,
                priceChangePercent24h = coinDetail.marketData.priceChangePercentage24hInCurrency[userCurrency.id]!!,
                priceChangePercent7d = coinDetail.marketData.priceChangePercentage7dInCurrency[userCurrency.id]!!,
                priceChangePercent14d = coinDetail.marketData.priceChangePercentage14dInCurrency[userCurrency.id]!!,
                priceChangePercent30d = coinDetail.marketData.priceChangePercentage30dInCurrency[userCurrency.id]!!,
                priceChangePercent60d = coinDetail.marketData.priceChangePercentage60dInCurrency[userCurrency.id]!!,
                priceChangePercent1y = coinDetail.marketData.priceChangePercentage1yInCurrency[userCurrency.id]!!,
                marketCap = coinDetail.marketData.marketCap[userCurrency.id]!!,
                marketCapChangePercent24h = coinDetail.marketData.marketCapChangePercentage24hInCurrency[userCurrency.id]!!,
            )
        }
    }
}