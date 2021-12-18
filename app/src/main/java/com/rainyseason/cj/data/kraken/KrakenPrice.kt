package com.rainyseason.cj.data.kraken

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KrakenPrice(
    @Json(name = "price")
    val price: Double
)
