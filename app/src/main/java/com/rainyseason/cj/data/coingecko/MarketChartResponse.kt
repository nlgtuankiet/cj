package com.rainyseason.cj.data.coingecko

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MarketChartResponse(
    @Json(name = "prices")
    val prices: List<List<Double>>,

    @Json(name = "market_caps")
    val marketCaps: List<List<Double>>,

    @Json(name = "total_volumes")
    val totalVolumes: List<List<Double>>,
)

fun MarketChartResponse.currentPrice(): Double? {
    return prices.lastOrNull { it.size == 2 }?.get(1)
}
