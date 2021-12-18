package com.rainyseason.cj.data.coingecko

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.common.reverseValue
import com.rainyseason.cj.data.Signal
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import kotlin.math.abs

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

class CoinGeckoServiceWrapper(
    private val service: CoinGeckoService,
) : CoinGeckoService by service {

    private fun Map<String, Double>.positive(): Map<String, Double> {
        return mapValues { abs(it.value) }
    }

    override suspend fun getCoinDetail(id: String): CoinDetailResponse {
        val original = service.getCoinDetail(id)
        if (DebugFlag.POSITIVE_WIDGET.isEnable) {
            val marketData = original.marketData
            return original.copy(
                marketData = marketData.copy(
                    priceChange24hInCurrency = marketData.priceChange24hInCurrency.positive(),
                    priceChangePercentage24hInCurrency = marketData
                        .priceChangePercentage24hInCurrency.positive(),
                    priceChangePercentage7dInCurrency = marketData
                        .priceChangePercentage7dInCurrency.positive(),
                    priceChangePercentage14dInCurrency = marketData
                        .priceChangePercentage14dInCurrency.positive(),
                    priceChangePercentage30dInCurrency = marketData
                        .priceChangePercentage30dInCurrency.positive(),
                    priceChangePercentage60dInCurrency = marketData
                        .priceChangePercentage60dInCurrency.positive(),
                    priceChangePercentage1yInCurrency = marketData
                        .priceChangePercentage1yInCurrency.positive(),
                    marketCapChangePercentage24hInCurrency = marketData
                        .marketCapChangePercentage24hInCurrency.positive(),
                )
            )
        }
        return original
    }

    override suspend fun getCoinMarkets(
        vsCurrency: String,
        perPage: Int,
        page: Int,
        sparkline: Boolean,
        forceCache: Boolean
    ): List<MarketsResponseEntry> {
        val original = service.getCoinMarkets(vsCurrency, perPage, page, sparkline, forceCache)
        if (DebugFlag.POSITIVE_WIDGET.isEnable) {
            return original.map { entry ->
                entry.copy(
                    priceChangePercentage24h = entry.priceChangePercentage24h?.let { p -> abs(p) }
                )
            }
        }
        return original
    }

    override suspend fun getMarketChart(
        id: String,
        vsCurrency: String,
        day: String
    ): MarketChartResponse {
        val original = service.getMarketChart(id, vsCurrency, day).run {
            copy(
                prices = prices.filterValidValue(),
                marketCaps = marketCaps.filterValidValue(),
                totalVolumes = totalVolumes.filterValidValue(),
            )
        }
        if (DebugFlag.POSITIVE_WIDGET.isEnable) {
            return original.run {
                copy(
                    prices = prices.reverseValueIfNegative(),
                    marketCaps = marketCaps.reverseValueIfNegative(),
                    totalVolumes = totalVolumes.reverseValueIfNegative(),
                )
            }
        }
        return original
    }
}

private fun List<List<Double>>.reverseValueIfNegative(): List<List<Double>> {
    if (size >= 2 && first()[1] > last()[1]) {
        return reverseValue()
    }
    return this
}

/**
 * Filter out invalid value
 * Ex: https://api.coingecko.com/api/v3/coins/bitcoin/market_chart/?vs_currency=brl&days=max
 * Search for null
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun List<List<Double>>.filterValidValue(): List<List<Double>> {
    @Suppress("SENSELESS_COMPARISON")
    return filter { it.size >= 2 && it[0] != null && it[1] != null && it[0] != 0.0 }
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
