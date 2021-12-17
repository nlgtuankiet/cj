package com.rainyseason.cj.data.ftx

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FtxService {

    @GET("markets")
    suspend fun getMarkets(): FtxResponse<List<FtxMarket>>

    @GET("markets/{id}")
    suspend fun getMarket(
        @Path("id") id: String
    ): FtxResponse<FtxMarketPrice>

    @GET("markets/{id}/candles")
    suspend fun getCandles(
        @Path("id") id: String,
        @Query("resolution") resolution: Int,
    ): FtxResponse<List<FtxCandle>>

    companion object {
        const val BASE_URL = "https://ftx.com/api/"
    }
}
