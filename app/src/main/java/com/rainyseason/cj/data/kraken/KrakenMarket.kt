package com.rainyseason.cj.data.kraken

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class KrakenMarket(
    @Json(name = "exchange")
    val exchange: Exchange,
    @Json(name = "currencyPair")
    val currencyPair: Pair
) {

    @JsonClass(generateAdapter = true)
    data class Exchange(
        @Json(name = "slug")
        val slug: String
    )

    @JsonClass(generateAdapter = true)
    data class Pair(
        @Json(name = "slug")
        val slug: String,
        @Json(name = "v3_slug")
        val v3Slug: String,
    )
}
