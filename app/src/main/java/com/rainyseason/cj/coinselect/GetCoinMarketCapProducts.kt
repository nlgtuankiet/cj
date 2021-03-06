package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.SchemeLoader
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.cmc.CmcService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCoinMarketCapProducts @Inject constructor(
    backendProductStore: BackendProductStore,
    private val cmcService: CmcService,
) {

    private val remoteSource = object : SchemeLoader.RemoteSource<BackendProduct> {
        override suspend fun get(fromIndex: Int, limit: Int): List<BackendProduct> {
            return cmcService.getMap(start = fromIndex + 1, limit = limit)
                .data.cryptoCurrencyMap.map { crypto ->
                    BackendProduct(
                        id = crypto.id,
                        symbol = crypto.symbol,
                        displayName = crypto.name,
                        backend = Backend.CoinMarketCap,
                        iconUrl = CmcService.getCmcIconUrl(crypto.id)
                    )
                }
        }
    }

    private val loader = ProductsLoader(
        backend = Backend.CoinMarketCap,
        backendProductStore = backendProductStore,
        remoteSource = remoteSource,
        loadScheme = listOf(1_000, 5_000, 10_000)
    )

    operator fun invoke(): Flow<List<BackendProduct>> {
        return loader.invoke()
    }
}
