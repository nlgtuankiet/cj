package com.rainyseason.cj.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.withTransaction
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.TraceParam
import com.rainyseason.cj.data.database.kv.KeyValueDao
import com.rainyseason.cj.data.database.kv.KeyValueDatabase
import com.rainyseason.cj.data.database.kv.KeyValueEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class KeyValueDatabaseMigrator @Inject constructor(
    private val daoProvider: Provider<KeyValueDao>,
    private val databaseProvider: Provider<KeyValueDatabase>,
    private val context: Context,
    private val traceManager: TraceManager,
) {
    private val dao: KeyValueDao
        get() = daoProvider.get()
    private val database: KeyValueDatabase
        get() = databaseProvider.get()

    private val listFiles = listOf(
        "coin_history",
        "coin_ticker_storage",
        "watch_storage",
        "common",
        "user_setting_storage",
    )

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val countDownLatch = CountDownLatch(1)

    fun migrate() {
        scope.launch {
            database.withTransaction {
                val migrated = dao.select("migrated")?.longValue == 1L
                if (migrated) {
                    deleteFiles()
                } else {
                    migrateToDatabase()
                    dao.insert(KeyValueEntry(key = "migrated", longValue = 1L))
                }
            }
            countDownLatch.countDown()
        }
    }

    private fun deleteFiles() {
        listFiles.map { name ->
            context.preferencesDataStoreFile(name)
        }.forEach {
            if (it.isFile && it.exists()) {
                it.delete()
            }
        }
    }

    private suspend fun migrateToDatabase() {
        val keyValues = mutableMapOf<Preferences.Key<*>, Any>()
        val fileToMigrate = listFiles.map { context.preferencesDataStoreFile(it) }
            .filter { it.isFile && it.exists() }
        if (fileToMigrate.isEmpty()) {
            return
        }
        traceManager.beginTrace(MigrateKeyValueTTI)
        fileToMigrate
            .map { file ->
                PreferenceDataStoreFactory.create(
                    corruptionHandler = null,
                    migrations = emptyList(),
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    produceFile = { file }
                ).data.first().asMap()
            }.forEach { map -> keyValues.putAll(map) }
        database.withTransaction {
            keyValues.forEach { (key: Preferences.Key<*>, value: Any) ->
                when (value) {
                    is String -> dao.insert(KeyValueEntry(key = key.name, stringValue = value))
                    is Int -> dao.insert(KeyValueEntry(key = key.name, longValue = value.toLong()))
                    is Double -> dao.insert(KeyValueEntry(key = key.name, doubleValue = value))
                    is Boolean -> dao.insert(
                        KeyValueEntry(
                            key = key.name,
                            longValue = if (value) 1 else 0
                        )
                    )
                    is Float -> dao.insert(
                        KeyValueEntry(
                            key = key.name,
                            doubleValue = value.toDouble()
                        )
                    )
                    is Long -> dao.insert(KeyValueEntry(key = key.name, longValue = value))
                }
            }
        }
        traceManager.endTrace(MigrateKeyValueTTI)
    }

    object MigrateKeyValueTTI : TraceParam(key = "migrate_key_value", name = "migrate_key_value")

    fun waitMigrate() {
        countDownLatch.await()
    }
}
