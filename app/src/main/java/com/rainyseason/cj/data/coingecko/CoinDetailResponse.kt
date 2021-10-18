package com.rainyseason.cj.data.coingecko

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CoinDetailResponse(
    @Json(name = "id")
    val id: String,

    @Json(name = "symbol")
    val symbol: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "image")
    val image: Image,

    @Json(name = "market_data")
    val marketData: MarketData,

    @Json(name = "market_cap_rank")
    val marketCapRank: Int?,

    @Json(name = "hashing_algorithm")
    val hashingAlgorithm: String,

) {

    @JsonClass(generateAdapter = true)
    data class Image(
        @Json(name = "large")
        val large: String
    )

    @JsonClass(generateAdapter = true)
    data class MarketData(
        @Json(name = "current_price")
        val currentPrice: Map<String, Double>,

        @Json(name = "price_change_percentage_24h_in_currency")
        val priceChangePercentage24hInCurrency: Map<String, Double>,

        @Json(name = "price_change_percentage_7d_in_currency")
        val priceChangePercentage7dInCurrency: Map<String, Double>,

        @Json(name = "price_change_percentage_14d_in_currency")
        val priceChangePercentage14dInCurrency: Map<String, Double>,

        @Json(name = "price_change_percentage_30d_in_currency")
        val priceChangePercentage30dInCurrency: Map<String, Double>,

        @Json(name = "price_change_percentage_60d_in_currency")
        val priceChangePercentage60dInCurrency: Map<String, Double>,

        @Json(name = "price_change_percentage_1y_in_currency")
        val priceChangePercentage1yInCurrency: Map<String, Double>,

        @Json(name = "market_cap")
        val marketCap: Map<String, Double>,

        @Json(name = "market_cap_change_percentage_24h_in_currency")
        val marketCapChangePercentage24hInCurrency: Map<String, Double>,

        @Json(name = "circulating_supply")
        val circulatingSupply: Double?,

        @Json(name = "total_supply")
        val totalSupply: Double?,

        @Json(name = "max_supply")
        val maxSupply: Double?,

        @Json(name = "ath")
        val ath: Map<String, Double>?,
    )
}
