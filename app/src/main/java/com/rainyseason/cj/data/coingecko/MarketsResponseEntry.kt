package com.rainyseason.cj.data.coingecko

import com.rainyseason.cj.common.model.Backend
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
) {
    val uniqueId: String = "${Backend.CoinGecko.id}_$id"
}
