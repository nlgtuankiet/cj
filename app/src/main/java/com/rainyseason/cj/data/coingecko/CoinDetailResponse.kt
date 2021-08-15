package com.rainyseason.cj.data.coingecko

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinDetailResponse(
    @Json(name = "id")
    val id: String,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "image")
    val image: Image,

    @Json(name = "market_data")
    val marketData: MarketData
) {

    @JsonClass(generateAdapter = true)
    data class Image(
        @Json(name = "large")
        val large: String
    )

    @JsonClass(generateAdapter = true)
    data class MarketData(
        @Json(name = "current_price")
        val currentPrice: Map<String, Double>,

        @Json(name = "price_change_percentage_24h")
        val priceChangePercentage24h: Double,

        @Json(name = "price_change_percentage_7d")
        val priceChangePercentage7d: Double,

        @Json(name = "price_change_percentage_14d")
        val priceChangePercentage14d: Double,
    )
}