package com.rainyseason.cj.coinselect

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.common.SchemeLoader
import com.rainyseason.cj.common.model.Backend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.threeten.bp.Instant
import java.util.concurrent.TimeUnit

class ProductsLoader(
    private val backend: Backend,
    private val backendProductStore: BackendProductStore,
    remoteSource: SchemeLoader.RemoteSource<BackendProduct>,
    private val loadScheme: List<Int>,
    private val cacheTimeMilis: Long = TimeUnit.MINUTES.toMillis(5),
) {
    private val loadMutex = Mutex()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val loader = SchemeLoader(
        localSource = object : SchemeLoader.LocalSource<Backend, BackendProduct> {
            override suspend fun get(key: Backend): List<BackendProduct>? {
                return backendProductStore.get(key)
            }

            override suspend fun set(key: Backend, value: List<BackendProduct>) {
                return backendProductStore.set(key, value)
            }
        },
        remoteSource = remoteSource,
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
            loadWithScheme(loadScheme)
        }
    }

    private suspend fun maybeLoad(block: suspend () -> Unit) {
        loadMutex.withLock {
            val updateAt = backendProductStore.getUpdateAt(backend)
            if (updateAt != null && updateAt.plusMillis(cacheTimeMilis) > Instant.now()) {
                return@withLock // skip load because the data is fresh
            }
            block()
        }
    }

    private suspend fun loadWithScheme(scheme: List<Int>) {
        try {
            loader.invoke(backend, scheme)
        } catch (ex: Exception) {
            // usually network error
            FirebaseCrashlytics.getInstance().recordException(ex)
        }
    }

    operator fun invoke(): Flow<List<BackendProduct>> {
        triggerIncrementalLoad()
        return backendProductStore.getFlow(backend)
    }
}
