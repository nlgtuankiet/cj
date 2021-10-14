package com.rainyseason.cj.widget.watch

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WatchDisplayData(
    @Json(name = "entries")
    val entries: List<WatchDisplayEntry>
)

@JsonClass(generateAdapter = true)
data class WatchDisplayEntry(
    @Json(name = "coin_id")
    val coinId: String,
    @Json(name = "content")
    val content: WatchDisplayEntryContent?
)

@JsonClass(generateAdapter = true)
data class WatchDisplayEntryContent(
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "graph")
    val graph: List<List<Double>>?,
    @Json(name = "price")
    val price: Double,
    @Json(name = "change_percent")
    val changePercent: Double?
)