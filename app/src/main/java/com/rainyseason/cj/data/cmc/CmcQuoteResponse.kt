package com.rainyseason.cj.data.cmc

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CmcQuoteResponse(
    val data: List<Data>
) {

    @JsonClass(generateAdapter = true)
    data class Data(
        val id: String,
        val name: String,
        val symbol: String,
        val quotes: List<Quote>
    ) {

        @JsonClass(generateAdapter = true)
        data class Quote(
            val price: Double,
            val percentChange24h: Double,
            val percentChange7d: Double,
            val percentChange30d: Double,
        )
    }
}
