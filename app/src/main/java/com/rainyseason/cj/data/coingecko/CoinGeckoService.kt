package com.rainyseason.cj.data.coingecko

import retrofit2.http.GET
import retrofit2.http.Path

interface CoinGeckoService {

    @GET("coins/{id}")
    suspend fun getCoinDetail(@Path("id") id: String): CoinDetailResponse
}