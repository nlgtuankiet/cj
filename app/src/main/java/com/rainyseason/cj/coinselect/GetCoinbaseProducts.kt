package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.SchemeLoader
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.coinbase.CoinbaseService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCoinbaseProducts @Inject constructor(
    backendProductStore: BackendProductStore,
    private val coinbaseService: CoinbaseService
) {

    private val remoteSource = object : SchemeLoader.RemoteSource<BackendProduct> {
        override suspend fun get(fromIndex: Int, limit: Int): List<BackendProduct> {
            return coinbaseService.getProducts()
                .map {
                    BackendProduct(
                        id = it.id,
                        symbol = it.id,
                        displayName = it.displayName,
                        backend = Backend.Coinbase,
                        iconUrl = Backend.Coinbase.iconUrl
                    )
                }
        }
    }

    private val loader = ProductsLoader(
        backend = Backend.Coinbase,
        backendProductStore = backendProductStore,
        remoteSource = remoteSource,
        loadScheme = listOf(Int.MAX_VALUE),
    )

    operator fun invoke(): Flow<List<BackendProduct>> {
        return loader.invoke()
    }
}
