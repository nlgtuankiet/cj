package com.rainyseason.cj.data.binance

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * interval 1m (limit 500 - 1000)
 *          3m
 *          5m
 *          15m
 *          30m
 *          1h
 *          2h
 *          4h
 *          6h
 *          8h
 *          12h
 *          1d
 *          3d
 *          1w
 *          1M
 *
 */
interface BinanceService {
    @GET("api/v3/exchangeInfo")
    suspend fun getSymbolDetail(@Query("symbol") symbol: String): SymbolInfoResponse

    @GET("/api/v3/klines")
    suspend fun getKLines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int,
    ): List<List<Double>>

    @GET("api/v3/ticker/price")
    suspend fun getTickerPrice(@Query("symbol") symbol: String): TickerPriceResponse

    companion object {
        const val BASE_URL = "https://api.binance.com/"
    }
}

class BinanceServiceWrapper(
    private val binanceService: BinanceService
) : BinanceService by binanceService {
    override suspend fun getKLines(
        symbol: String,
        interval: String,
        limit: Int
    ): List<List<Double>> {
        val response = binanceService.getKLines(symbol, interval, limit)
        return response.filter { it.size == 12 && it[0] > 0 && it[6] > 0 }
    }
}
