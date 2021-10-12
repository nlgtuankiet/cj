package com.rainyseason.cj.widget.watch

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
annotation class Watch

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class WatchWidgetRepository @Inject constructor(
    @Watch
    private val storage: DataStore<Preferences>,
    private val moshi: Moshi,
) {
    private fun configKey(widgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("config_$widgetId")
    }

    private fun displayDataKey(widgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("display_data_$widgetId")
    }

    private val configAdapter = moshi.adapter(WatchConfig::class.java)
    private val displayDataAdapter = moshi.adapter(WatchDisplayData::class.java)

    fun getDisplayDataStream(widgetId: Int): Flow<WatchDisplayData> {
        return storage.data.mapNotNull { it[displayDataKey(widgetId)] }
            .map { displayDataAdapter.fromJson(it)!! }
            .distinctUntilChanged()
    }

    fun getConfigStream(widgetId: Int): Flow<WatchConfig> {
        return storage.data.mapNotNull { it[configKey(widgetId)] }
            .map { configAdapter.fromJson(it)!! }
            .distinctUntilChanged()
    }

    suspend fun getConfig(widgetId: Int): WatchConfig? {
        return storage.data.first()[configKey(widgetId)]?.let { configAdapter.fromJson(it) }
    }

    suspend fun clearDisplayData(widgetId: Int) {
        storage.edit {
            it.remove(displayDataKey(widgetId))
        }
    }

    suspend fun setConfig(widgetId: Int, config: WatchConfig) {
        storage.edit {
            it[configKey(widgetId)] = configAdapter.toJson(config)
        }
    }

    suspend fun clearAllData(widgetId: Int) {
        storage.edit {
            it.remove(configKey(widgetId))
            it.remove(displayDataKey(widgetId))
        }
    }
}