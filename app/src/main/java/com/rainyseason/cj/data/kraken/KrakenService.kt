package com.rainyseason.cj.data.kraken

import com.rainyseason.cj.data.common.ResultResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Qualifier

@Qualifier
annotation class Kraken

interface KrakenService {

    @GET("fixtures/js/data/markets")
    suspend fun getMarkets(): ResultResponse<Map<String, KrakenMarket>>

    @GET("markets/kraken/{id}/price")
    suspend fun getPrice(
        @Path("id") id: String
    ): ResultResponse<KrakenPrice>

    @GET("markets/kraken/{id}/ohlc")
    suspend fun getGraph(
        @Path("id") id: String,
        @Query("periods") period: Int
    ): ResultResponse<Map<String, List<List<Double>>>>

    companion object {
        const val BASE_URL = "https://api.cryptowat.ch/"
    }
}
