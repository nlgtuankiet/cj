package com.rainyseason.cj.ticker

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TickerWidgetDisplay(
    @Json(name = "id")
    val id: String,

    @Json(name = "icon_url")
    val iconUrl: String,

    @Json(name = "current_price")
    val currentPrice: String,
)