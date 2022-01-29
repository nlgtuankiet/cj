package com.rainyseason.cj.data.luno

import retrofit2.http.GET
import retrofit2.http.Query

interface LunoWebService {

    @GET("ticker")
    suspend fun ticker(
        @Query("pair") id: String
    ): LunoTicker

    /**
     * @param resolution
     * minutes: 1 5 15 30 60 180 240 480
     * hour: 1D
     *
     * @param from in seconds
     */
    @GET("udf/history")
    suspend fun ohlc(
        @Query("symbol") id: String,
        @Query("resolution") resolution: String,
        @Query("from") from: Long, // seconds
        @Query("to") to: Long,
    ): LunoOHLC

    companion object {
        const val BASE_URL = "https://ajax.luno.com/ajax/1/"
    }
}

class LunoWebServiceWrapper(
    private val service: LunoWebService
) : LunoWebService by service {
    override suspend fun ohlc(id: String, resolution: String, from: Long, to: Long): LunoOHLC {
        val response = service.ohlc(id, resolution, from, to)
        val max = response.run { time.size.coerceAtLeast(close.size) }
        val times = response.time.take(max)
            .map { it * 1000 }
        val closes = response.close.take(max)
        return LunoOHLC(
            time = times,
            close = closes,
        )
    }
}

interface LunoService {
    @GET("exchange/1/markets")
    suspend fun markets(): LunoMarketResponse

    companion object {
        const val BASE_URL = "https://api.luno.com/api/"
    }
}
