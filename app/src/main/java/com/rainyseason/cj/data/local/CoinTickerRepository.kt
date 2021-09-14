package com.rainyseason.cj.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rainyseason.cj.common.CoinTickerStorage
import com.rainyseason.cj.data.UserSetting
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.rainyseason.cj.ticker.addBitmap
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class CoinTickerRepository @Inject constructor(
    moshi: Moshi,
    private val dataStore: CoinTickerStorage,
    private val context: Context,
    private val userSettingRepository: UserSettingRepository,
) {
    private val displayAdapter = moshi.adapter(CoinTickerDisplayData::class.java)
    private val configAdapter = moshi.adapter(CoinTickerConfig::class.java)

    suspend fun clearAllData(widgetId: Int) {
        Timber.d("clearAllData $widgetId")
        dataStore.edit {
            it.remove(displayDataKey(widgetId))
            it.remove(configKey(widgetId))
        }
    }

    suspend fun clearDisplayData(widgetId: Int) {
        dataStore.edit {
            it.remove(displayDataKey(widgetId))
        }
    }

    @Suppress("UnnecessaryVariable")
    suspend fun getAllDataIds(): List<Int> {
        val keys = dataStore.data.map {
            it.asMap().keys.filterIsInstance<Preferences.Key<String>>().map { k -> k.name }
        }.first()
        val regex = """_(\d+)${'$'}""".toRegex()
        val ids = keys.mapNotNull {
            regex.find(it)?.groupValues?.get(1)?.toInt()
        }.toSet().toList().sorted()
        return ids
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
        val userSetting = UserSetting(
            currencyCode = widgetConfig.currency,
            refreshInterval = widgetConfig.refreshInterval,
            refreshIntervalUnit = widgetConfig.refreshIntervalUnit,
            amountDecimals = widgetConfig.numberOfAmountDecimal,
            roundToMillion = widgetConfig.roundToMillion,
            showCurrencySymbol = widgetConfig.showCurrencySymbol,
            showThousandsSeparator = widgetConfig.showThousandsSeparator,
            numberOfChangePercentDecimal = widgetConfig.numberOfChangePercentDecimal,
        )
        userSettingRepository.setUserSetting(userSetting)
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