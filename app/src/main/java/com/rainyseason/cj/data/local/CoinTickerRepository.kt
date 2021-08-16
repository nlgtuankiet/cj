package com.rainyseason.cj.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rainyseason.cj.ticker.TickerWidgetConfig
import com.rainyseason.cj.ticker.TickerWidgetConfigJsonAdapter
import com.rainyseason.cj.ticker.TickerWidgetDisplayData
import com.rainyseason.cj.ticker.TickerWidgetDisplayDataJsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class CoinTickerRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val moshi: Moshi,
) {
    private val displayAdapter = TickerWidgetDisplayDataJsonAdapter(moshi = moshi)
    private val configAdapter = TickerWidgetConfigJsonAdapter(moshi = moshi)

    suspend fun allKey(): Set<String> {
        return dataStore.data.first().asMap().keys
            .filterIsInstance<Preferences.Key<String>>()
            .map { it.name }
            .toSet()
    }

    suspend fun getDisplayData(widgetId: Int): TickerWidgetDisplayData? {
        val key = displayDataKey(widgetId)
        val data = dataStore.data.first()[key] ?: return null
        return displayAdapter.fromJson(data)!!
    }

    suspend fun setDisplayData(widgetId: Int, data: TickerWidgetDisplayData) {
        val key = displayDataKey(widgetId)
        val data = displayAdapter.toJson(data)
        dataStore.edit { it[key] = data }
    }

    fun getDisplayDataStream(widgetId: Int): Flow<TickerWidgetDisplayData> {
        val key = displayDataKey(widgetId)
        return dataStore.data
            .mapNotNull { prefs: Preferences ->
                prefs[key]
            }
            .map { displayAdapter.fromJson(it)!! }
    }

    private fun displayDataKey(widgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("ticker_widget_display_data_${widgetId}")
    }


    private fun configKey(widgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("ticker_widget_config_${widgetId}")
    }

    suspend fun setConfig(widgetId: Int, widgetConfig: TickerWidgetConfig) {
        val key = configKey(widgetId)
        val data = configAdapter.toJson(widgetConfig)
        dataStore.edit { it[key] = data }
    }

    suspend fun getConfig(widgetId: Int): TickerWidgetConfig? {
        val key = configKey(widgetId)
        val data = dataStore.data.first()[key] ?: return null
        return configAdapter.fromJson(data)!!
    }

    fun getConfigStream(widgetId: Int): Flow<TickerWidgetConfig> {
        val key = configKey(widgetId)
        return dataStore.data
            .mapNotNull { prefs: Preferences ->
                prefs[key]
            }
            .map { configAdapter.fromJson(it)!! }
    }
}