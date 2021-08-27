package com.rainyseason.cj.data.coingecko

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinGeckoService {

    @GET("coins/{id}")
    suspend fun getCoinDetail(@Path("id") id: String): CoinDetailResponse

    @GET("coins/markets")
    suspend fun getCoinMarkets(
        @Query("vs_currency") vsCurrency: String,
        @Query("per_page") perPage: Int,
    ): List<MarketsResponseEntry>

    @GET("coins/list")
    suspend fun getCoinList(): List<CoinListEntry>
}