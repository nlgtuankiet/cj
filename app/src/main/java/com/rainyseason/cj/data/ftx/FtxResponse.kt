package com.rainyseason.cj.data.ftx

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FtxMarket(
    @Json(name = "name")
    val id: String,
)

@JsonClass(generateAdapter = true)
data class FtxMarketPrice(
    @Json(name = "price")
    val price: Double,
)

@JsonClass(generateAdapter = true)
data class FtxCandle(
    @Json(name = "time")
    val time: Double,
    @Json(name = "close")
    val close: Double
)
