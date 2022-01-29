package com.rainyseason.cj.data.luno

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LunoMarketResponse(
    @Json(name = "markets")
    val markets: List<Market>,
) {

    @JsonClass(generateAdapter = true)
    data class Market(
        @Json(name = "market_id")
        val id: String,
        @Json(name = "base_currency")
        val base: String,
        @Json(name = "counter_currency")
        val quote: String,
    )
}
