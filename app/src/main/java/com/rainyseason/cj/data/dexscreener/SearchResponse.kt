package com.rainyseason.cj.data.dexscreener

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse(
    @Json(name = "pairs")
    val pairs: List<Pair>
)
