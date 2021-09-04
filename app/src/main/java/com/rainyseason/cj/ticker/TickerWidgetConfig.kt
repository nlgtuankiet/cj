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

    @Json(name = "number_of_price_decimal")
    val numberOfPriceDecimal: Int? = null,

    @Json(name = "number_of_change_percent_decimal")
    val numberOfChangePercentDecimal: Int? = null,

    @Json(name = "refresh_interval")
    val refreshInterval: Long = 15,

    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.MINUTES,

    @Json(name = "theme")
    val theme: String = Theme.AUTO,

    @Json(name = "extra_size")
    val extraSize: Int = 0,

    @Json(name = "show_thousands_separator")
    val showThousandsSeparator: Boolean = true,

    @Json(name = "bottom_content_type")
    val bottomContentType: String = BottomContentType.PRICE,

    @Json(name = "price_change_interval")
    val priceChangeInterval: String = ChangeInterval._24H,

    @Json(name = "market_cap_change_interval")
    val marketCapChangeInterval: String = ChangeInterval._24H
) {

    val bottomInterval: String
        get() = when (bottomContentType) {
            BottomContentType.PRICE -> priceChangeInterval
            BottomContentType.MARKET_CAP -> marketCapChangeInterval
            else -> error("unknown $bottomContentType")
        }
}