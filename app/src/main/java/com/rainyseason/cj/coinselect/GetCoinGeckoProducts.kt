package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.CoinListEntry
import com.rainyseason.cj.data.coingecko.MarketsResponseEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import javax.inject.Inject

class GetCoinGeckoProducts @Inject constructor(
    private val coinGeckoService: CoinGeckoService,
) {
    operator fun invoke(): Flow<List<BackendProduct>> {
        return flow {
            supervisorScope {
                val marketAsync = async {
                    coinGeckoService
                        .getCoinMarkets(vsCurrency = "usd", perPage = 1000)
                }
                val listAsync = async {
                    coinGeckoService.getCoinList()
                }

                try {
                    Timber.d("load cache")
                    val cachedMarket = async {
                        coinGeckoService
                            .getCoinMarkets(vsCurrency = "usd", perPage = 1000, forceCache = true)
                    }
                    val cacheList = async {
                        coinGeckoService.getCoinList(forceCache = true)
                    }
                    val merged = merge(cachedMarket.await(), cacheList.await())
                    Timber.d("emit cache")
                    emit(merged)
                } catch (ex: Exception) {
                    if (ex is CancellationException) {
                        throw ex
                    }
                    Timber.d("emit cache error ${ex.message}")
                }

                emit(merge(marketAsync.await(), null))
                emit(merge(marketAsync.await(), listAsync.await()))
            }
        }
    }

    private fun merge(
        markets: List<MarketsResponseEntry>,
        coinList: List<CoinListEntry>?,
    ): List<BackendProduct> {
        val marketsProducts = markets.map { entry ->
            BackendProduct(
                id = entry.id,
                symbol = entry.symbol,
                displayName = entry.name,
                backend = Backend.CoinGecko,
                iconUrl = entry.image,
            )
        }

        val coinListProducts = coinList.orEmpty().map { entry ->
            BackendProduct(
                id = entry.id,
                symbol = entry.symbol,
                displayName = entry.name,
                backend = Backend.CoinGecko,
                iconUrl = Backend.CoinGecko.iconUrl,
            )
        }

        if (coinList == null) {
            return marketsProducts
        }

        return (marketsProducts + coinListProducts).distinctBy { it.id }
    }
}
