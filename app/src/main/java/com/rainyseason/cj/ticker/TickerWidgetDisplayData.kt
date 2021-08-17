package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Combine data with config to render UI
 *
 */
@JsonClass(generateAdapter = true)
data class TickerWidgetDisplayData(
    @Json(name = "icon_url")
    val iconUrl: String,

    @Transient
    val iconBitmap: Bitmap? = null,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "price")
    val price: Double,

    @Json(name = "change_24h_percent")
    val change24hPercent: Double,

    @Json(name = "change_7d_percent")
    val change7dPercent: Double,

    @Json(name = "change_14d_percent")
    val change14dPercent: Double,
)