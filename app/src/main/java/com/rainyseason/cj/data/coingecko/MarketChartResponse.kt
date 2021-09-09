package com.rainyseason.cj.data.coingecko

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class MarketChartResponse(
    @Json(name = "prices")
    val prices: List<List<Double>>
)