package com.rainyseason.cj.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rainyseason.cj.ticker.TickerWidgetConfig
import com.rainyseason.cj.ticker.TickerWidgetConfigJsonAdapter
import com.rainyseason.cj.ticker.TickerWidgetDisplayConfigJsonAdapter
import com.rainyseason.cj.ticker.TickerWidgetDisplayData
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class CoinTickerRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val moshi: Moshi,
) {
    private val adapter = TickerWidgetDisplayConfigJsonAdapter(moshi = moshi)
    private val configAdapter = TickerWidgetConfigJsonAdapter(moshi = moshi)

    suspend fun getDisplayData(widgetId: Int): TickerWidgetDisplayData? {
        val key = tickerDisplayConfigKey(widgetId)
        val data = dataStore.data.first()[key] ?: return null
        return adapter.fromJson(data)!!
    }

    suspend fun setDisplayConfig(widgetId: Int, data: TickerWidgetDisplayData) {
        val key = tickerDisplayConfigKey(widgetId)
        val data = adapter.toJson(data)
        dataStore.edit { it[key] = data }
    }

    private fun tickerDisplayConfigKey(widgetId: Int): Preferences.Key<String> {
        return stringPreferencesKey("ticker_widget_display_config_${widgetId}")
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

}