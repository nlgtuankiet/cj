package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.SchemeLoader
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.data.cmc.CmcService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.threeten.bp.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetCoinMarketCapProducts @Inject constructor(
    private val backendProductStore: BackendProductStore,
    private val cmcService: CmcService,
) {
    private val loadMutex = Mutex()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
    operator fun invoke(): Flow<List<BackendProduct>> {
        triggerIncrementalLoad()
        return backendProductStore.getFlow(Backend.CoinMarketCap)
    }

    private val loader = SchemeLoader(
        localSource = object : SchemeLoader.LocalSource<Backend, BackendProduct> {
            override suspend fun get(key: Backend): List<BackendProduct>? {
                return backendProductStore.get(key)
            }

            override suspend fun set(key: Backend, value: List<BackendProduct>) {
                return backendProductStore.set(key, value)
            }
        },
        remoteSource = object : SchemeLoader.RemoteSource<BackendProduct> {
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
        },
        idSelector = object : SchemeLoader.IdSelector<BackendProduct, String> {
            override fun invoke(key: BackendProduct): String {
                return key.id
            }
        }
    )

    private fun triggerIncrementalLoad() {
        scope.launch {
            incrementalLoad()
        }
    }

    private suspend fun incrementalLoad() {
        maybeLoad {
            loadWithScheme(listOf(1_000, 5_000, 10_000))
        }
    }

    private suspend fun maybeLoad(block: suspend () -> Unit) {
        loadMutex.withLock {
            val updateAt = backendProductStore.getUpdateAt(Backend.CoinMarketCap)
            if (updateAt != null && updateAt.plusSeconds(5L * 60) > Instant.now()) {
                return@withLock // skip load because the data is fresh
            }
            block()
        }
    }

    private suspend fun loadWithScheme(scheme: List<Int>) {
        loader.invoke(Backend.CoinMarketCap, scheme)
    }
}
