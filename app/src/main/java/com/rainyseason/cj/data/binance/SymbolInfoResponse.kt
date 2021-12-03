package com.rainyseason.cj.data.binance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SymbolInfoResponse(
    @Json(name = "symbols")
    val symbols: List<Symbol>
) {
    /**
     * BNBBTC -> baseAsset BNB quoteAsset BTC
     */
    @JsonClass(generateAdapter = true)
    data class Symbol(
        @Json(name = "symbol")
        val symbol: String,

        @Json(name = "baseAsset")
        val baseAsset: String,

        @Json(name = "quoteAsset")
        val quoteAsset: String,
    ) {
        fun displayName(): String {
            return "$baseAsset/$quoteAsset"
        }
    }
}
