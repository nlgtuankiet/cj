package com.rainyseason.cj.widget.watch

import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WatchConfig(
    @Json(name = "widget_id")
    val widgetId: Int,

    @Json(name = "interval")
    val interval: TimeInterval = TimeInterval.I_24H,

    @Json(name = "currency")
    val currency: String = "usd",

    @Json(name = "type")
    val layout: WatchWidgetLayout = WatchWidgetLayout.Watch4x2,

    @Json(name = "theme")
    val theme: Theme = Theme.Dark,
) {
    companion object {
        const val MIN_WIDGET_WIDTH = 330
    }
}