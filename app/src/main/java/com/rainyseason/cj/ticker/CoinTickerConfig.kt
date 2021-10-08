package com.rainyseason.cj.ticker

import com.rainyseason.cj.common.Theme
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.concurrent.TimeUnit

/**
 * Map 1-1 between widget_id -> configs for that widget in order to generate display config
 */
@JsonClass(generateAdapter = true)
data class CoinTickerConfig(
    @Json(name = "widget_id")
    val widgetId: Int,

    @Json(name = "coin_id")
    val coinId: String,

    @Json(name = "number_of_price_decimal")
    val numberOfAmountDecimal: Int? = 2,

    @Json(name = "number_of_change_percent_decimal")
    val numberOfChangePercentDecimal: Int? = 1,

    @Json(name = "refresh_interval")
    val refreshInterval: Long = 60,

    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.MINUTES,

    @Json(name = "theme")
    val theme: String = Theme.AUTO,

    @Json(name = "show_thousands_separator")
    val showThousandsSeparator: Boolean = true,

    @Json(name = "bottom_content_type")
    val bottomContentType: String = BottomContentType.PRICE,

    @Json(name = "change_interval")
    val changeInterval: String = ChangeInterval._24H,

    @Json(name = "layout")
    val layout: String = Layout.GRAPH,

    @Json(name = "click_action")
    val clickAction: String = ClickAction.REFRESH,

    @Json(name = "show_currency_symbol")
    val showCurrencySymbol: Boolean = true,

    @Json(name = "currency")
    val currency: String,

    @Json(name = "round_to_million")
    val roundToMillion: Boolean = true,

    @Json(name = "show_battery_warning")
    val showBatteryWarning: Boolean = true,

    @Json(name = "size_adjustment")
    val sizeAdjustment: Int = 0,

    @Json(name = "background_transparency")
    val backgroundTransparency: Int = 0,

    @Json(name = "hide_decimal_on_large_price")
    val hideDecimalOnLargePrice: Boolean = true,

    @Json(name = "amount")
    val amount: Double? = 1.0,
) {

    fun getTrackingParams(): Map<String, Any?> {
        return mapOf(
            "widget_id" to widgetId,
            "coin_id" to coinId,
            "number_of_price_decimal" to numberOfAmountDecimal,
            "number_of_change_percent_decimal" to numberOfChangePercentDecimal,
            "refresh_interval_seconds" to refreshIntervalUnit.toSeconds(refreshInterval),
            "theme" to theme,
            "show_thousands_separator" to showThousandsSeparator,
            "bottom_content_type" to bottomContentType,
            "change_interval" to changeInterval,
            "layout" to layout,
            "click_action" to clickAction,
            "show_currency_symbol" to showCurrencySymbol,
            "currency" to currency,
            "round_to_million" to roundToMillion,
            "show_battery_ưarning" to showBatteryWarning,
            "size_adjustment" to sizeAdjustment,
            "background_transparency" to backgroundTransparency,
            "hide_decimal_on_large_price" to hideDecimalOnLargePrice,
            "amount" to amount,
        )
    }

    object Layout {
        const val DEFAULT = "default"
        const val GRAPH = "graph"
        const val COIN360 = "coin360"
        const val MINI = "mini"
    }

    object ClickAction {
        const val REFRESH = "refresh"
        const val SETTING = "setting"
        const val SWITCH_PRICE_MARKET_CAP = "switch_price_market_cap"
    }

    object Action {
        const val SWITCH_ACTION = "com.rainyseason.cj.widget.cointicker.switch"
    }
}
