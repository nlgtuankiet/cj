package com.rainyseason.cj.data.database.kv

import androidx.room.withTransaction
import com.rainyseason.cj.data.KeyValueDatabaseMigrator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class KeyValueStore @Inject constructor(
    private val daoProvider: Provider<KeyValueDao>,
    private val databaseProvider: Provider<KeyValueDatabase>,
    private val keyValueDatabaseMigrator: KeyValueDatabaseMigrator,
) {
    private val dao: KeyValueDao
        get() = daoProvider.get()

    suspend fun getString(key: String): String? {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.select(key)?.stringValue
    }

    fun getStringFlow(key: String): Flow<String?> {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.selectFlow(key).map { it?.stringValue }.distinctUntilChanged()
    }

    suspend fun setString(key: String, value: String?) {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.insert(KeyValueEntry(key = key, stringValue = value))
    }

    suspend fun getLong(key: String): Long? {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.select(key)?.longValue
    }

    fun getLongFlow(key: String): Flow<Long?> {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.selectFlow(key).map { it?.longValue }.distinctUntilChanged()
    }

    suspend fun setLong(key: String, value: Long?) {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.insert(KeyValueEntry(key = key, longValue = value))
    }

    suspend fun getDouble(key: String): Double? {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.select(key)?.doubleValue
    }

    fun getDoubleFlow(key: String): Flow<Double?> {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.selectFlow(key).map { it?.doubleValue }.distinctUntilChanged()
    }

    suspend fun setDouble(key: String, value: Double?) {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.insert(KeyValueEntry(key = key, doubleValue = value))
    }

    suspend fun getBoolean(key: String): Boolean? {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.select(key)?.longValue?.let { it == 1L }
    }

    suspend fun setBoolean(key: String, value: Boolean?) {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.insert(KeyValueEntry(key = key, longValue = value?.let { if (it) 1L else 0L }))
    }

    suspend fun delete(key: String) {
        keyValueDatabaseMigrator.waitMigrate()
        return dao.delete(key)
    }

    suspend fun <R> withTransaction(block: suspend () -> R): R {
        keyValueDatabaseMigrator.waitMigrate()
        return databaseProvider.get().withTransaction(block)
    }
}
