package com.rainyseason.cj.ticker

import com.rainyseason.cj.common.Theme
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.concurrent.TimeUnit

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

    @Json(name = "number_of_price_decimal")
    val numberOfPriceDecimal: Int? = null,

    @Json(name = "number_of_change_percent_decimal")
    val numberOfChangePercentDecimal: Int? = null,

    @Json(name = "refresh_interval")
    val refreshInterval: Long = 15,

    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.MINUTES,

    @Json(name = "theme")
    val theme: String = Theme.DEFAULT
) {
    val isComplete: Boolean
        get() = coinId.isNotEmpty()
}