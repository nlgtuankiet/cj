package com.rainyseason.cj.data.database.kv

import kotlinx.coroutines.flow.Flow
import org.threeten.bp.Instant
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class KeyValueStore @Inject constructor(
    private val daoProvider: Provider<KeyValueDao>
) : KeyValueDao {
    private val dao: KeyValueDao by lazy { daoProvider.get() }

    override suspend fun insert(entry: KeyValueEntry) {
        dao.insert(entry)
    }

    override suspend fun select(key: String): KeyValueEntry? {
        return dao.select(key)
    }

    override fun selectFlow(key: String): Flow<KeyValueEntry?> {
        return dao.selectFlow(key)
    }

    override fun selectUpdateAt(key: String): Instant? {
        return dao.selectUpdateAt(key)
    }
}
