package com.rainyseason.cj.data.coingecko

import com.rainyseason.cj.common.model.Backend
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinListEntry(
    @Json(name = "id")
    val id: String,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "name")
    val name: String,
) {
    val uniqueId: String = "${Backend.CoinGecko.id}_$id"
}
