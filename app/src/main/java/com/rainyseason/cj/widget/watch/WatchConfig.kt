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

    @Json(name = "refresh_interval")
    val refreshInterval: Long = 1,

    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.HOURS,

    @Json(name = "size_adjustment")
    val sizeAdjustment: Int = 0,

    @Json(name = "background_transparency")
    val backgroundTransparency: Int = 0,

    @Json(name = "number_of_price_decimal")
    val numberOfAmountDecimal: Int = 2,

    @Json(name = "round_to_million")
    val roundToMillion: Boolean = true,

    @Json(name = "show_currency_symbol")
    val showCurrencySymbol: Boolean = true,

    @Json(name = "show_thousands_separator")
    val showThousandsSeparator: Boolean = true,

    @Json(name = "hide_decimal_on_large_price")
    val hideDecimalOnLargePrice: Boolean = true,

    @Json(name = "number_of_change_percent_decimal")
    val numberOfChangePercentDecimal: Int = 1,

    @Json(name = "show_battery_warning")
    val showBatteryWarning: Boolean = true,

    ) {
    fun getTrackingParams(): Map<String, Any?> {
        // TODO
        return emptyMap()
    }

    companion object {
        const val MIN_WIDGET_WIDTH = 330
    }
}