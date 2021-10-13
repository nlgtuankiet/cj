package com.rainyseason.cj.widget.watch

import com.rainyseason.cj.common.model.TimeInterval
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WatchConfig(
    @Json(name = "interval")
    val interval: TimeInterval = TimeInterval.I_24H,

    @Json(name = "currency")
    val currency: String = "usd",
)