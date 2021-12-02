package com.rainyseason.cj.ticker

import android.graphics.Bitmap
import com.rainyseason.cj.common.model.Exchange
import com.rainyseason.cj.common.model.TimeInterval
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Combine data with config to render UI
 *
 */
@JsonClass(generateAdapter = true)
data class CoinTickerDisplayData(
    @Json(name = "icon_url")
    val iconUrl: String,

    @Transient
    val iconBitmap: Bitmap? = null,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "price")
    val price: Double?, // null when coin is in preview

    @Json(name = "price_change_percent")
    val priceChangePercent: Double?,

    @Json(name = "price_graph")
    val priceGraph: List<List<Double>>? = null,
) {

    data class LoadParam(
        val coinId: String,
        val exchange: Exchange?,
        val currency: String,
        val changeInterval: TimeInterval,
    )

    fun getAmount(config: CoinTickerConfig): Double? {
        return if (price == null) {
            null
        } else {
            price * (config.amount ?: 1.0)
        }
    }
}
