package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.SchemeLoader
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.kraken.KrakenService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetKrakenProducts @Inject constructor(
    backendProductStore: BackendProductStore,
    private val krakenService: KrakenService,
) {

    private val remoteSource = object : SchemeLoader.RemoteSource<BackendProduct> {
        override suspend fun get(fromIndex: Int, limit: Int): List<BackendProduct> {
            return krakenService.getMarkets().result
                .filter { (_, v) ->
                    v.exchange.slug == "kraken"
                }
                .map { (_, v) ->
                    BackendProduct(
                        id = v.currencyPair.slug,
                        symbol = v.currencyPair.v3Slug,
                        displayName = v.currencyPair.v3Slug,
                        backend = Backend.Kraken,
                        iconUrl = Backend.Kraken.iconUrl
                    )
                }
        }
    }

    private val loader = ProductsLoader(
        backend = Backend.Kraken,
        backendProductStore = backendProductStore,
        remoteSource = remoteSource,
    )

    operator fun invoke(): Flow<List<BackendProduct>> {
        return loader.invoke()
    }
}
