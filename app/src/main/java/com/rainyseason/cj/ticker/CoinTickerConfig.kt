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

    @Json(name = "change_interval")
    val changeInterval: String = ChangeInterval._24H,

    @Json(name = "layout")
    val layout: String = Layout.DEFAULT,

    @Json(name = "click_action")
    val clickAction: String = ClickAction.REFRESH,

    @Json(name = "show_currency_symbol")
    val showCurrencySymbol: Boolean = true,
) {

    object Layout {
        const val DEFAULT = "default"
        const val GRAPH = "graph"
        const val COIN360 = "coin360"
    }

    object ClickAction {
        const val REFRESH = "refresh"
        const val SETTING = "setting"
    }
}