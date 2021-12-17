package com.rainyseason.cj.data.coinbase

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CoinbaseService {

    @GET("products")
    suspend fun getProducts(): List<CoinbaseProduct>

    /**
     * [timestamp, price_low, price_high, price_open, price_close]
     */
    @GET("products/{id}/candles")
    suspend fun getCandles(
        @Path("id")
        id: String,
        @Query("granularity")
        granularity: Int, // 1m 5m 15m 1h 6h 1d in seconds
    ): List<List<Double>>

    @GET("products/{id}/ticker")
    suspend fun getTicker(
        @Path("id")
        id: String
    ): CoinbaseTicker

    companion object {
        const val BASE_URL = "https://api.pro.coinbase.com/"
    }
}

class CoinbaseServiceWrapper(
    private val service: CoinbaseService
) : CoinbaseService by service {
    override suspend fun getCandles(id: String, granularity: Int): List<List<Double>> {
        return service.getCandles(id, granularity)
            .filter { it.size == 6 && it[0] > 0 }
            .reversed()
    }
}
