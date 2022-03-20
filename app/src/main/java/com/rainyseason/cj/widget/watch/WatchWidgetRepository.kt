package com.rainyseason.cj.widget.watch

import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.widget.WidgetRefreshEventInterceptor
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("BlockingMethodInNonBlockingContext")
@Singleton
class WatchWidgetRepository @Inject constructor(
    moshi: Moshi,
    private val keyValueStore: KeyValueStore,
    private val userSettingRepository: UserSettingRepository,
    private val widgetRefreshEventInterceptor: WidgetRefreshEventInterceptor,
) {
    private fun configKey(widgetId: Int): String {
        return "config_$widgetId"
    }

    private fun displayDataKey(widgetId: Int): String {
        return "display_data_$widgetId"
    }

    private val configAdapter = moshi.adapter(WatchConfig::class.java)
    private val displayDataAdapter = moshi.adapter(WatchDisplayData::class.java)

    fun getDisplayDataStream(widgetId: Int): Flow<WatchDisplayData> {
        return keyValueStore.getStringFlow(displayDataKey(widgetId))
            .filterNotNull()
            .map { displayDataAdapter.fromJson(it)!! }
    }

    suspend fun getDisplayData(widgetId: Int): WatchDisplayData? {
        return keyValueStore.getString(displayDataKey(widgetId))?.let {
            displayDataAdapter.fromJson(it)
        }
    }

    fun getConfigStream(widgetId: Int): Flow<WatchConfig> {
        return keyValueStore.getStringFlow(configKey(widgetId))
            .filterNotNull()
            .map { configAdapter.fromJson(it)!!.ensureValid() }
            .distinctUntilChanged()
    }

    suspend fun getConfig(widgetId: Int): WatchConfig? {
        return keyValueStore.getString(configKey(widgetId))
            ?.let { configAdapter.fromJson(it) }?.ensureValid()
    }

    suspend fun clearDisplayData(widgetId: Int) {
        keyValueStore.delete(displayDataKey(widgetId))
    }

    suspend fun setDisplayData(widgetId: Int, displayData: WatchDisplayData) {
        keyValueStore.setString(
            displayDataKey(widgetId),
            displayDataAdapter.toJson(displayData)
        )
    }

    suspend fun setConfig(widgetId: Int, config: WatchConfig) {
        keyValueStore.setString(configKey(widgetId), configAdapter.toJson(config))
        val currentSetting = userSettingRepository.getUserSetting()
        val userSetting = currentSetting.copy(
            currencyCode = config.currency,
            refreshInterval = config.refreshInterval,
            refreshIntervalUnit = config.refreshIntervalUnit,
            amountDecimals = config.numberOfAmountDecimal,
            roundToMillion = config.roundToMillion,
            showCurrencySymbol = config.showCurrencySymbol,
            showThousandsSeparator = config.showThousandsSeparator,
            numberOfChangePercentDecimal = config.numberOfChangePercentDecimal,
            sizeAdjustment = config.sizeAdjustment,
        )
        userSettingRepository.setUserSetting(userSetting)
    }

    suspend fun clearAllData(widgetId: Int) {
        keyValueStore.withTransaction {
            keyValueStore.delete(configKey(widgetId))
            keyValueStore.delete(displayDataKey(widgetId))
            widgetRefreshEventInterceptor.deleteHash(widgetId)
        }
    }
}
