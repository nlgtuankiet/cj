package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.SchemeLoader
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.luno.LunoService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetLunoProducts @Inject constructor(
    private val lunoService: LunoService,
    backendProductStore: BackendProductStore
) {

    private val remoteSource = object : SchemeLoader.RemoteSource<BackendProduct> {
        override suspend fun get(fromIndex: Int, limit: Int): List<BackendProduct> {
            return lunoService.markets().markets
                .map { market ->
                    BackendProduct(
                        id = market.id,
                        symbol = market.id,
                        displayName = "${market.base}/${market.quote}",
                        backend = Backend.Luno,
                        iconUrl = Backend.Luno.iconUrl
                    )
                }
        }
    }

    private val loader = ProductsLoader(
        backend = Backend.Luno,
        backendProductStore = backendProductStore,
        remoteSource = remoteSource,
    )

    operator fun invoke(): Flow<List<BackendProduct>> {
        return loader.invoke()
    }
}
