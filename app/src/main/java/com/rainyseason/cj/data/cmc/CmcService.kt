package com.rainyseason.cj.data.cmc

import retrofit2.http.GET
import retrofit2.http.Query

interface CmcService {
    @GET("map/all?cryptoAux=is_active&exchangeAux=is_active&listing_status=active,untracked")
    suspend fun getMap(
        @Query("start") start: Int = 1,
        @Query("limit") limit: Int = 10_000
    ): CmcMapResponse

    @GET("cryptocurrency/quote/latest")
    suspend fun getQuote(
        @Query("id") id: String,
        @Query("convertId") convertId: String,
    ): CmcQuoteResponse

    @GET("cryptocurrency/detail/chart")
    suspend fun getChart(
        @Query("id") id: String,
        @Query("convertId") convertId: String,
        @Query("range") range: String,
    ): CmcChartResponse

    companion object {
        const val BASE_URL = "https://api.coinmarketcap.com/data-api/v3/"
        fun getCmcIconUrl(id: String): String {
            return "https://s2.coinmarketcap.com/static/img/coins/128x128/$id.png"
        }
    }
}
