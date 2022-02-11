package com.rainyseason.cj.common.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Coin(
    @Json(name = "id")
    val id: String,

    @Json(name = "backend")
    val backend: Backend = Backend.CoinGecko,

    @Json(name = "network")
    val network: String? = null,

    @Json(name = "dex")
    val dex: String? = null,
)
