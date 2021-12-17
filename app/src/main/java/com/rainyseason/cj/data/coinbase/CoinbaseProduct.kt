package com.rainyseason.cj.data.coinbase

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinbaseProduct(
    @Json(name = "id")
    val id: String,
    @Json(name = "display_name")
    val displayName: String
)
