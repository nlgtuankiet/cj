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

    @Json(name = "click_action")
    val clickAction: WatchClickAction = WatchClickAction.OpenWatchlist

) {
    fun getTrackingParams(): Map<String, Any?> {
        return mapOf(
            "widget_id" to widgetId,
            "number_of_price_decimal" to numberOfAmountDecimal,
            "number_of_change_percent_decimal" to numberOfChangePercentDecimal,
            "refresh_interval_seconds" to refreshIntervalUnit.toSeconds(refreshInterval),
            "theme" to theme,
            "show_thousands_separator" to showThousandsSeparator,
            "change_interval" to interval.id,
            "layout" to layout,
            "click_action" to clickAction,
            "show_currency_symbol" to showCurrencySymbol,
            "currency" to currency,
            "round_to_million" to roundToMillion,
            "show_battery_ưarning" to showBatteryWarning,
            "size_adjustment" to sizeAdjustment,
            "background_transparency" to backgroundTransparency,
            "hide_decimal_on_large_price" to hideDecimalOnLargePrice,
        )
    }

    companion object {
        const val MIN_WIDGET_WIDTH = 330
    }
}