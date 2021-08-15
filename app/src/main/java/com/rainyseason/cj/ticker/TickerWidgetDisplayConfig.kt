package com.rainyseason.cj.ticker

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TickerWidgetDisplayConfig(
    @Json(name = "id")
    val id: String,

    @Json(name = "icon_url")
    val iconUrl: String,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "current_price")
    val currentPrice: Double,

    @Json(name = "currency_symbol")
    val currencySymbol: String,

    // ex: left $1.0
    // ex: right 16500vnd
    @Json(name = "currency_symbol_on_the_left")
    val currencySymbolOnTheLeft: Boolean = true,

    @Json(name = "separator")
    val separator: String,

    @Json(name = "price_change_percentage_24h")
    val priceChangePercentage24h: Double,

    @Json(name = "price_change_percentage_7d")
    val priceChangePercentage7d: Double
)