package com.rainyseason.cj.data.dexscreener

import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface DexScreenerService {

    @GET("u/search/pairs")
    suspend fun search(@Query("q") query: String): SearchResponse

    @GET("u/chart/bars/{platformId}/{pairId}")
    suspend fun bars(
        @Path("platformId") platformId: String,
        @Path("pairId") pairId: String,
        @Query("from") from: Long,
        @Query("to") to: Long,
        @Query("res") res: String,
        @Query("cb") cb: Long,
    ): BarResponse

    // 0{"sid":"ZuxK6_ZxHaDFo92-C68t","upgrades":["websocket"],"pingInterval":25000,"pingTimeout":20000}
    @GET("u/ws/screener/?EIO=4&transport=polling")
    suspend fun detail1(
        @Query("t") random: String
    ): ResponseBody

    // &t=Nx&sid=ZuxK6_ZxHaDFo92-C68t
    /**
     * body:
     */
    @POST("u/ws/screener/?EIO=4&transport=polling")
    suspend fun detail2(
        @Query("t") random: String,
        @Query("sid") sid: String,
        @Body body: RequestBody,
    ): ResponseBody

    @GET("u/ws/screener/?EIO=4&transport=polling")
    suspend fun detail3(
        @Query("t") random: String,
        @Query("sid") sid: String,
    ): ResponseBody

    companion object {
        const val BASE_URL = "https://io.dexscreener.com/"

        fun getDetailBody(platform: String, coinId: String): RequestBody {
            return "40/u/ws/screener/pair/$platform/$coinId,".toRequestBody()
        }
    }
}

class DexScreenerServiceWrapper(
    private val service: DexScreenerService
) : DexScreenerService by service {
    override suspend fun bars(
        platformId: String,
        pairId: String,
        from: Long,
        to: Long,
        res: String,
        cb: Long
    ): BarResponse {
        val response = service.bars(platformId, pairId, from, to, res, cb)
        return response.copy(
            bars = response.bars.sortedBy { it.timestamp }
        )
    }
}
