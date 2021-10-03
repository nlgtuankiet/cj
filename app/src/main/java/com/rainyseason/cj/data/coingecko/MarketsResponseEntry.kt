package com.rainyseason.cj.data.coingecko

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MarketsResponseEntry(
    @Json(name = "id")
    val id: String,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "image")
    val image: String,

    @Json(name = "price_change_percentage_24h")
    val priceChangePercentage24h: Double?,

    @Json(name = "current_price")
    val currentPrice: Double,

    @Json(name = "sparkline_in_7d")
    val sparklineIn7d: SparklineIn7d?,
) {
    @JsonClass(generateAdapter = true)
    data class SparklineIn7d(
        @Json(name = "price")
        val price: List<Double>,
    )
}