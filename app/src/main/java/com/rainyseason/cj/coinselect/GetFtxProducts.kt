package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.SchemeLoader
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.ftx.FtxService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetFtxProducts @Inject constructor(
    backendProductStore: BackendProductStore,
    private val ftxService: FtxService,
) {

    private val remoteSource = object : SchemeLoader.RemoteSource<BackendProduct> {
        override suspend fun get(fromIndex: Int, limit: Int): List<BackendProduct> {
            return ftxService.getMarkets().result
                .map {
                    BackendProduct(
                        id = it.id,
                        symbol = it.id,
                        displayName = it.id,
                        backend = Backend.Ftx,
                        iconUrl = Backend.Ftx.iconUrl
                    )
                }
        }
    }

    private val loader = ProductsLoader(
        backend = Backend.Ftx,
        backendProductStore = backendProductStore,
        remoteSource = remoteSource,
        loadScheme = listOf(Int.MAX_VALUE),
    )

    operator fun invoke(): Flow<List<BackendProduct>> {
        return loader.invoke()
    }
}
