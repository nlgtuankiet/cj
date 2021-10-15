package com.rainyseason.cj.widget.watch

import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class WatchConfig(
    @Json(name = "widget_id")
    val widgetId: Int,

    @Json(name = "interval")
    val interval: TimeInterval = TimeInterval.I_24H,

    @Json(name = "currency")
    val currency: String = "usd",

    @Json(name = "type")
    val layout: WatchWidgetLayout,

    @Json(name = "theme")
    val theme: Theme = Theme.Auto,

    @Json(name = "change_percent_decimal")
    val changePercentDecimal: Int = 1,

    @Json(name = "refresh_interval")
    val refreshInterval: Long = 1,

    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.HOURS,

    ) {
    companion object {
        const val MIN_WIDGET_WIDTH = 330
    }
}