package com.rainyseason.cj.data.coinbase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinbaseTicker(
    @Json(name = "price")
    val price: Double
)
