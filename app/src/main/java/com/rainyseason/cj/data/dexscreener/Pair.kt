package com.rainyseason.cj.data.dexscreener

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Pair(
    @Json(name = "platformId")
    val platformId: String?,
    @Json(name = "dexId")
    val dexId: String?,
    @Json(name = "pairAddress")
    val pairAddress: String,
    @Json(name = "baseToken")
    val baseToken: BaseToken,
    @Json(name = "quoteTokenSymbol")
    val quoteTokenSymbol: String,
    @Json(name = "priceChange")
    val priceChange: PriceChange?
) {
    fun symbol(): String {
        return "${baseToken.symbol.uppercase()}/${quoteTokenSymbol.uppercase()}"
    }
}

@JsonClass(generateAdapter = true)
data class BaseToken(
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "name")
    val name: String,
)

@JsonClass(generateAdapter = true)
data class PriceChange(
    @Json(name = "h24")
    val h24: Double,
)
