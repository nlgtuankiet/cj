package com.rainyseason.cj.ticker

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Map 1-1 between widget_id -> configs for that widget in order to generate display config
 */
@JsonClass(generateAdapter = true)
data class TickerWidgetConfig(
    @Json(name = "widget_id")
    val widgetId: Int,

    @Json(name = "coin_id")
    val coinId: String,

    @Json(name = "show_change_24h")
    val showChange24h: Boolean,

    @Json(name = "show_change_7d")
    val showChange7d: Boolean,

    @Json(name = "show_change_14d")
    val showChange14d: Boolean,
) {
    val isComplete: Boolean
        get() = coinId.isNotEmpty()
}