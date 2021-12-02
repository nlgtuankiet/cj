package com.rainyseason.cj.ticker

import com.rainyseason.cj.R
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.model.Exchange
import com.rainyseason.cj.common.model.TimeInterval
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

    @Json(name = "exchange")
    val exchange: Exchange? = null,

    @Json(name = "number_of_price_decimal")
    val numberOfAmountDecimal: Int? = 2,

    @Json(name = "number_of_change_percent_decimal")
    val numberOfChangePercentDecimal: Int? = 1,

    @Json(name = "refresh_interval")
    val refreshInterval: Long = 1,

    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.HOURS,

    @Json(name = "theme")
    val theme: String = Theme.AUTO,

    @Json(name = "show_thousands_separator")
    val showThousandsSeparator: Boolean = true,

    @Json(name = "change_interval")
    val changeInterval: TimeInterval = TimeInterval.I_24H,

    @Json(name = "layout")
    val layout: String = Layout.GRAPH,

    @Json(name = "click_action")
    val clickAction: String = ClickAction.OPEN_COIN_DETAIL,

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

    val isExchange: Boolean
        get() = exchange != null

    fun asDataLoadParams(): CoinTickerDisplayData.LoadParam {
        return CoinTickerDisplayData.LoadParam(
            coinId = coinId,
            exchange = exchange,
            currency = currency,
            changeInterval = changeInterval,
        )
    }

    fun getRefreshMilis(): Long {
        return refreshIntervalUnit.toMillis(refreshInterval)
    }

    fun getTrackingParams(): Map<String, Any?> {
        return mapOf(
            "widget_id" to widgetId,
            "coin_id" to coinId,
            "number_of_price_decimal" to numberOfAmountDecimal,
            "number_of_change_percent_decimal" to numberOfChangePercentDecimal,
            "refresh_interval_seconds" to refreshIntervalUnit.toSeconds(refreshInterval),
            "theme" to theme,
            "show_thousands_separator" to showThousandsSeparator,
            "change_interval" to changeInterval.id,
            "layout" to layout,
            "click_action" to clickAction,
            "show_currency_symbol" to showCurrencySymbol,
            "currency" to currency,
            "round_to_million" to roundToMillion,
            "show_battery_warning" to showBatteryWarning,
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
        const val COIN360_NANO = "coin360_mini"
        const val MINI = "mini"
        const val NANO = "nano"
        const val ICON_SMALL = "icon_small"

        val clazzToLayout = mapOf(
            CoinTickerProviderDefault::class.java.name to DEFAULT,
            CoinTickerProviderGraph::class.java.name to GRAPH,
            CoinTickerProviderCoin360::class.java.name to COIN360,
            CoinTickerProviderCoin360Mini::class.java.name to COIN360_NANO,
            CoinTickerProviderMini::class.java.name to MINI,
            CoinTickerProviderNano::class.java.name to NANO,
            CoinTickerProviderIconSmall::class.java.name to ICON_SMALL,
        )

        private val layoutToLayoutRes = mapOf(
            DEFAULT to R.layout.widget_coin_ticker_2x2,
            GRAPH to R.layout.widget_coin_ticker_2x2,
            COIN360 to R.layout.widget_coin_ticker_2x2,
            COIN360_NANO to R.layout.widget_coin_ticker_1x1,
            MINI to R.layout.widget_coin_ticker_2x1,
            NANO to R.layout.widget_coin_ticker_1x1,
            ICON_SMALL to R.layout.widget_coin_ticker_2x1,
        )

        fun fromComponentName(name: String): String {
            return clazzToLayout[name]!!
        }

        fun getLayoutRes(layout: String): Int {
            return layoutToLayoutRes[layout]!!
        }
    }

    object ClickAction {
        const val REFRESH = "refresh"
        const val SETTING = "setting"
        const val SWITCH_PRICE_MARKET_CAP = "switch_price_market_cap"
        const val OPEN_COIN_DETAIL = "open_coin_detail"
    }

    object Action {
        const val SWITCH_ACTION = "com.rainyseason.cj.widget.cointicker.switch"
    }
}
