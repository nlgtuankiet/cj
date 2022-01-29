package com.rainyseason.cj.data.luno

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LunoTicker(
    val bid: Double,
    val ask: Double,
    @Json(name = "currency_pair")
    val currencyPair: Pair,
) {
    @JsonClass(generateAdapter = true)
    data class Pair(
        @Json(name = "Base")
        val base: String,
        @Json(name = "Counter")
        val quote: String,
    )
}
