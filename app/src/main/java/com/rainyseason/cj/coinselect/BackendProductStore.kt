package com.rainyseason.cj.coinselect

import android.content.Context
import com.rainyseason.cj.common.model.Backend
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import okio.buffer
import okio.sink
import okio.source
import org.threeten.bp.Instant
import java.io.File
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class BackendProductStore @Inject constructor(
    moshi: Moshi,
    private val context: Context,
) {
    private val adapter: JsonAdapter<List<BackendProduct>> = moshi.adapter(
        Types.newParameterizedType(List::class.java, BackendProduct::class.java)
    )

    private val fileReadWriteLock = Any()

    private val memoryCache: MutableMap<Backend, List<BackendProduct>> = Collections
        .synchronizedMap(mutableMapOf<Backend, List<BackendProduct>>())

    private val memoryCacheChangeListener = mutableMapOf<Backend, MutableList<() -> Unit>>()

    private fun addListener(backend: Backend, listener: () -> Unit) {
        synchronized(memoryCacheChangeListener) {
            val currentSet = memoryCacheChangeListener[backend] ?: mutableListOf()
            currentSet.add(listener)
            memoryCacheChangeListener[backend] = currentSet
        }
        listener.invoke()
    }

    private fun removeListener(backend: Backend, listener: () -> Unit) {
        synchronized(memoryCacheChangeListener) {
            memoryCacheChangeListener[backend]?.remove(listener)
        }
    }

    private fun getBackendStoreDir(): File {
        val dir = File(context.cacheDir, "backend_product")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun get(backend: Backend): List<BackendProduct>? {
        return synchronized(memoryCache) {
            val cache = memoryCache[backend].orEmpty()
            if (cache.isNotEmpty()) {
                return cache
            }
            val file = File(getBackendStoreDir(), backend.id)
            if (!file.exists() || !file.isFile) {
                return null
            }
            synchronized(fileReadWriteLock) {
                file.source().buffer().use { adapter.fromJson(it) }?.also {
                    memoryCache[backend] = it
                }
            }
        }
    }

    fun set(backend: Backend, products: List<BackendProduct>) {
        val file = File(getBackendStoreDir(), backend.id)
        memoryCache[backend] = products
        synchronized(memoryCacheChangeListener) {
            memoryCacheChangeListener[backend]?.forEach { it.invoke() }
        }
        synchronized(fileReadWriteLock) {
            file.sink().buffer().use { adapter.toJson(it, products) }
        }
    }

    fun getUpdateAt(backend: Backend): Instant? {
        val file = File(getBackendStoreDir(), backend.id)
        if (file.exists() && file.isFile) {
            return Instant.ofEpochMilli(file.lastModified())
        }
        return null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getFlow(backend: Backend): Flow<List<BackendProduct>> {
        return callbackFlow {
            val listener = {
                val products = get(backend)
                if (!products.isNullOrEmpty()) {
                    trySend(products)
                }
            }
            addListener(backend, listener)
            awaitClose {
                removeListener(backend, listener)
            }
        }.distinctUntilChanged()
    }
}
