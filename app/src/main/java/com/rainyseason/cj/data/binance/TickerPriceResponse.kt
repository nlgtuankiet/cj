package com.rainyseason.cj.data.binance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TickerPriceResponse(
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "price")
    val price: Double,
)
