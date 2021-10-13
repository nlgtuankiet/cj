package com.rainyseason.cj.widget.watch

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WatchDisplayData(
    @Json(name = "entries")
    val entries: Map<String, WatchDisplayDataEntry>
)

@JsonClass(generateAdapter = true)
data class WatchDisplayDataEntry(
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "chart")
    val chart: List<List<Double>>?,
    @Json(name = "price")
    val price: Double,
    @Json(name = "change_percent")
    val changePercent: Double?
)