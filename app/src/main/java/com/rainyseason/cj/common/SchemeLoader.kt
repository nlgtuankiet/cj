package com.rainyseason.cj.common

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class SchemeLoader<T, V, I>(
    private val localSource: LocalSource<T, V>,
    private val remoteSource: RemoteSource<V>,
    private val idSelector: IdSelector<V, I>
) {

    suspend operator fun invoke(key: T, scheme: List<Int>) {
        val oldProducts = localSource.get(key).orEmpty()
        var newProducts = emptyList<V>()
        var latestProducts = emptyList<V>()
        var loadCount = 0
        var shouldContinue = true
        while (currentCoroutineContext().isActive && shouldContinue) {
            val limit = scheme[loadCount.coerceAtMost(scheme.lastIndex)]
            val latestBatch = remoteSource.get(newProducts.size, limit)
            if (latestBatch.isEmpty()) {
                shouldContinue = false
                continue
            }
            newProducts = newProducts + latestBatch
            latestProducts = (newProducts + oldProducts)
                .distinctBy { idSelector.invoke(it) }
            localSource.set(key, latestProducts)
            if (latestBatch.size < limit) {
                shouldContinue = false
                continue
            }
            loadCount++
        }
        if (newProducts != latestProducts) {
            localSource.set(key, newProducts)
        }
    }

    interface IdSelector<V, I> {
        operator fun invoke(key: V): I
    }

    interface LocalSource<T, V> {
        suspend fun get(key: T): List<V>?
        suspend fun set(key: T, value: List<V>)
    }

    interface RemoteSource<V> {
        suspend fun get(fromIndex: Int, limit: Int): List<V>
    }
}
