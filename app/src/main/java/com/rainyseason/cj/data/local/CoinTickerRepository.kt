package com.rainyseason.cj.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerConfigJsonAdapter
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.rainyseason.cj.ticker.CoinTickerDisplayDataJsonAdapter
import com.rainyseason.cj.ticker.addBitmap
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class CoinTickerRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val context: Context,
    private val moshi: Moshi,
) {
    private val displayAdapter = CoinTickerDisplayDataJsonAdapter(moshi = moshi)
    private val configAdapter = CoinTickerConfigJsonAdapter(moshi = moshi)

    suspend fun clearAllData(widgetId: Int) {
        dataStore.edit {
            it.remove(displayDataKey(widgetId))
            it.remove(configKey(widgetId))
        }
    }

    suspend fun getDisplayData(widgetId: Int): CoinTickerDisplayData? {
        val key = displayDataKey(widgetId)
        val data = dataStore.data.first()[key] ?: return null
        val entity = displayAdapter.fromJson(data)!!
        return entity.addBitmap(context)
    }

    suspend fun setDisplayData(widgetId: Int, data: CoinTickerDisplayData) {
        val key = displayDataKey(widgetId)
        val data = displayAdapter.toJson(data)
        dataStore.edit { it[key] = data }
    }

    fun getDisplayDataStream(widgetId: Int): Flow<CoinTickerDisplayData> {
        val key = displayDataKey(widgetId)
        return dataStore.data
            .mapNotNull { prefs: Preferences ->
                prefs[key]
            }
            .map {
                displayAdapter.fromJson(it)!!.addBitmap(context)
            }
    }

    private fun displayDataKey(widgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("ticker_widget_display_data_${widgetId}")
    }


    private fun configKey(widgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("ticker_widget_config_${widgetId}")
    }

    suspend fun setConfig(widgetId: Int, widgetConfig: CoinTickerConfig) {
        val key = configKey(widgetId)
        val data = configAdapter.toJson(widgetConfig)
        dataStore.edit { it[key] = data }
    }

    suspend fun getConfig(widgetId: Int): CoinTickerConfig? {
        val key = configKey(widgetId)
        val data = dataStore.data.first()[key] ?: return null
        return configAdapter.fromJson(data)!!
    }

    fun getConfigStream(widgetId: Int): Flow<CoinTickerConfig> {
        val key = configKey(widgetId)
        return dataStore.data
            .mapNotNull { prefs: Preferences ->
                prefs[key]
            }
            .map { configAdapter.fromJson(it)!! }
    }
}