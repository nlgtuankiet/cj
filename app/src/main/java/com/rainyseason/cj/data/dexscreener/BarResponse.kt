package com.rainyseason.cj.data.dexscreener

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BarResponse(
    val bars: List<Bar>
) {
    @JsonClass(generateAdapter = true)
    data class Bar(
        val timestamp: Double,
        val close: Double,
        val closeUsd: Double,
    )
}
