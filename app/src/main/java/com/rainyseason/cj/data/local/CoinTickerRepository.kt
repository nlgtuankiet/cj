package com.rainyseason.cj.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rainyseason.cj.ticker.TickerWidgetDisplayConfig
import com.rainyseason.cj.ticker.TickerWidgetDisplayConfigJsonAdapter
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class CoinTickerRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val moshi: Moshi,
) {
    val adapter = TickerWidgetDisplayConfigJsonAdapter(moshi = moshi)

    suspend fun getConfig(widgetId: String): TickerWidgetDisplayConfig? {
        val key = tickerKey(widgetId)
        val data = dataStore.data.first()[key] ?: return null
        return adapter.fromJson(data)
    }

    private fun tickerKey(widgetId: String): Preferences.Key<String> {
        return stringPreferencesKey("ticker_widget_display_config_${widgetId}")
    }

    suspend fun setConfig(widgetId: String, config: TickerWidgetDisplayConfig) {
        val key = tickerKey(widgetId)
        val data = adapter.toJson(config)
        dataStore.edit { it[key] = data }
    }
}