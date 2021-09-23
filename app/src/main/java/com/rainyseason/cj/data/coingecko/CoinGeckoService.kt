package com.rainyseason.cj.data.coingecko

import com.rainyseason.cj.data.Signal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
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
        @Query(Signal.FORCE_CACHE) forceCache: Boolean = false,
    ): List<MarketsResponseEntry>

    @GET("coins/list")
    suspend fun getCoinList(
        @Query(Signal.FORCE_CACHE) forceCache: Boolean = false,
    ): List<CoinListEntry>

    @GET("coins/{id}/market_chart/")
    suspend fun getMarketChart(
        @Path("id") id: String,
        @Query(value = "vs_currency") vsCurrency: String,
        @Query(value = "days") day: Int,
    ): MarketChartResponse

    companion object {
        const val BASE_URL = "https://api.coingecko.com/api/v3/"
    }
}


private fun <T> fastResponseFlow(
    cacheProvider: suspend () -> T,
    networkProvider: suspend () -> T,
): Flow<T> {
    return flow {
        try {
            val cacheReponse = cacheProvider.invoke()
            emit(cacheReponse)
        } catch (ex: Exception) {

        }
        val realResponse = networkProvider.invoke()
        emit(realResponse)
    }.distinctUntilChanged()
}

fun CoinGeckoService.getCoinMarketsFlow(
    vsCurrency: String,
    perPage: Int,
): Flow<List<MarketsResponseEntry>> {
    return fastResponseFlow(
        cacheProvider = {
            getCoinMarkets(
                vsCurrency = vsCurrency,
                perPage = perPage,
                forceCache = true,
            )
        },
        networkProvider = {
            getCoinMarkets(
                vsCurrency = vsCurrency,
                perPage = perPage,
                forceCache = false,
            )
        }
    )
}

fun CoinGeckoService.getCoinListFlow(): Flow<List<CoinListEntry>> {
    return fastResponseFlow(
        cacheProvider = {
            getCoinList(
                forceCache = true,
            )
        },
        networkProvider = {
            getCoinList(
                forceCache = false,
            )
        }
    )
}