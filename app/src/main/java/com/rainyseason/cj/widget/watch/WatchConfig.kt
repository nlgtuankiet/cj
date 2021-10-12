package com.rainyseason.cj.widget.watch

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WatchConfig(
    @Json(name = "holder")
    val holder: Int = 1,
)