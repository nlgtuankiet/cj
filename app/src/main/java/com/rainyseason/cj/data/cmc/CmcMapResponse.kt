package com.rainyseason.cj.data.cmc

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CmcMapResponse(
    val data: Data,
) {

    @JsonClass(generateAdapter = true)
    data class Data(
        val cryptoCurrencyMap: List<CryptoCurrency>
    ) {

        @JsonClass(generateAdapter = true)
        data class CryptoCurrency(
            val id: String,
            val name: String,
            val symbol: String,
        )
    }
}
