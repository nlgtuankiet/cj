package com.rainyseason.cj.data.coingecko

import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        @Query("page") page: Int = 1,
        @Query("sparkline") sparkline: Boolean = false,
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
        @Query(value = "days") day: String,
    ): MarketChartResponse

    companion object {
        const val BASE_URL = "https://api.coingecko.com/api/v3/"
    }
}

suspend fun CoinGeckoService.getMarketChartWithFilter(
    @Path("id") id: String,
    @Query(value = "vs_currency") vsCurrency: String,
    @Query(value = "days") day: String,
): MarketChartResponse {
    val raw = getMarketChart(id, vsCurrency, day)
    return raw.copy(
        prices = raw.prices.filterValidValue(),
        marketCaps = raw.marketCaps.filterValidValue(),
        totalVolumes = raw.totalVolumes.filterValidValue(),
    )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<List<Double>>.filterValidValue(): List<List<Double>> {
    @Suppress("SENSELESS_COMPARISON")
    return filter { it.size == 2 && it[0] != null && it[1] != null && it[0] != 0.0 }
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
            FirebaseCrashlytics.getInstance().recordException(ex)
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
