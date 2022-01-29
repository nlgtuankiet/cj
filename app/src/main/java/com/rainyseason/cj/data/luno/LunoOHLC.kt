package com.rainyseason.cj.data.luno

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @param time API return in seconds but we converted to milis
 */
@JsonClass(generateAdapter = true)
data class LunoOHLC(
    @Json(name = "t")
    val time: List<Double>,
    @Json(name = "c")
    val close: List<Double>,
)
