package com.rainyseason.cj.ticker

import androidx.core.os.BuildCompat
import com.rainyseason.cj.common.CurrencyInfo
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.Theme
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.tracking.EventParamKey
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

    @Json(name = "backend")
    val backend: Backend = Backend.CoinGecko,

    @Json(name = "number_of_price_decimal")
    val numberOfAmountDecimal: Int? = 2,

    @Json(name = "number_of_change_percent_decimal")
    val numberOfChangePercentDecimal: Int? = 1,

    @Json(name = "refresh_interval")
    val refreshInterval: Long = 1,

    @Json(name = "refresh_interval_unit")
    val refreshIntervalUnit: TimeUnit = TimeUnit.HOURS,

    @Json(name = "theme")
    val theme: Theme = Theme.Auto,

    @Json(name = "show_thousands_separator")
    val showThousandsSeparator: Boolean = true,

    @Json(name = "change_interval")
    val changeInterval: TimeInterval = TimeInterval.I_24H,

    @Json(name = "layout")
    val layout: CoinTickerLayout = CoinTickerLayout.Graph2x2,

    @Json(name = "click_action")
    val clickAction: String = ClickAction.OPEN_COIN_DETAIL,

    @Json(name = "show_currency_symbol")
    val showCurrencySymbol: Boolean = true,

    @Json(name = "currency")
    val currency: String = CurrencyInfo.USD.code,

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

    @Json(name = "full_size")
    val fullSize: Boolean = true,

    @Json(name = "show_notification")
    val showNotification: Boolean = true,

    @Json(name = "network")
    val network: String? = null,

    @Json(name = "dex")
    val dex: String? = null,
) {

    companion object {
        val DEFAULT_FOR_PREVIEW = CoinTickerConfig(
            widgetId = Int.MAX_VALUE,
            coinId = "bitcoin",
            backend = Backend.CoinGecko,
            layout = CoinTickerLayout.Graph2x2,
            currency = "usd",
            showNotification = true,
        )
    }

    fun getCoin(): Coin {
        return Coin(
            id = coinId,
            backend = backend,
            network = network,
            dex = dex,
        )
    }

    fun ensureValid(): CoinTickerConfig {
        return ensureClickAction()
            .ensureChangeInterval()
            .ensureShowCurrency()
            .ensureTheme()
            .ensureCurrency()
    }

    private fun ensureCurrency(): CoinTickerConfig {
        val currencySupported = backend.supportedCurrency.any { it.code == currency }
        return if (currencySupported) {
            this
        } else {
            val currency = backend.supportedCurrency.firstOrNull { it.code == "usd" }
                ?: CurrencyInfo.NONE
            copy(currency = currency.code)
        }
    }

    private fun ensureTheme(): CoinTickerConfig {
        return if (theme.isMaterialYou && !BuildCompat.isAtLeastS()) {
            copy(theme = Theme.Auto)
        } else {
            this
        }
    }

    private fun ensureShowCurrency(): CoinTickerConfig {
        return if (backend.isExchange && showCurrencySymbol) {
            copy(showCurrencySymbol = false)
        } else {
            this
        }
    }

    private fun ensureClickAction(): CoinTickerConfig {
        return if (backend.isDefault) {
            this
        } else {
            if (clickAction == ClickAction.OPEN_COIN_DETAIL) {
                copy(clickAction = ClickAction.REFRESH)
            } else {
                this
            }
        }
    }

    private fun ensureChangeInterval(): CoinTickerConfig {
        return if (changeInterval in backend.supportedTimeRange) {
            this
        } else {
            copy(changeInterval = TimeInterval.I_24H)
        }
    }

    fun asDataLoadParams(): CoinTickerDisplayData.LoadParam {
        return CoinTickerDisplayData.LoadParam(
            coinId = coinId,
            backend = backend,
            currency = currency,
            changeInterval = changeInterval,
            network = network,
            dex = dex,
        )
    }

    fun getRefreshMilis(): Long {
        return refreshIntervalUnit.toMillis(refreshInterval)
    }

    fun getTrackingParams(): Map<String, Any?> {
        return mapOf(
            EventParamKey.WIDGET_ID to widgetId,
            "coin_id" to coinId,
            "backend" to backend.id,
            "number_of_price_decimal" to numberOfAmountDecimal,
            "number_of_change_percent_decimal" to numberOfChangePercentDecimal,
            "refresh_interval_seconds" to refreshIntervalUnit.toSeconds(refreshInterval),
            "theme" to theme.id,
            "show_thousands_separator" to showThousandsSeparator,
            "change_interval" to changeInterval.id,
            "layout" to layout.id,
            "click_action" to clickAction,
            "show_currency_symbol" to showCurrencySymbol,
            "currency" to currency,
            "round_to_million" to roundToMillion,
            "show_battery_warning" to showBatteryWarning,
            "size_adjustment" to sizeAdjustment,
            "background_transparency" to backgroundTransparency,
            "hide_decimal_on_large_price" to hideDecimalOnLargePrice,
            "show_notification" to showNotification,
            "full_size" to fullSize,
            "amount" to amount,
            "network" to network,
            "dex" to dex,
        )
    }

    object ClickAction {
        const val REFRESH = "refresh"
        const val SETTING = "setting"

        @Deprecated(message = "Will remove this after some versio")
        const val SWITCH_PRICE_MARKET_CAP = "switch_price_market_cap"
        const val OPEN_COIN_DETAIL = "open_coin_detail"
    }
}
