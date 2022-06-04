package com.rainyseason.cj.data.local

import android.content.Context
import com.rainyseason.cj.common.model.getWidgetIds
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.rainyseason.cj.ticker.CoinTickerLayout
import com.rainyseason.cj.ticker.usecase.CreateDefaultTickerWidgetConfig
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import com.rainyseason.cj.widget.WidgetRefreshEventInterceptor
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@Suppress("BlockingMethodInNonBlockingContext")
class CoinTickerRepository @Inject constructor(
    moshi: Moshi,
    private val context: Context,
    private val userSettingRepository: UserSettingRepository,
    private var keyValueStore: KeyValueStore,
    private val widgetRefreshEventInterceptor: WidgetRefreshEventInterceptor,
    private val tracker: Tracker,
    private val getDefaultTickerWidgetConfig: CreateDefaultTickerWidgetConfig,
) {
    private val displayAdapter = moshi.adapter(CoinTickerDisplayData::class.java)
    private val configAdapter = moshi.adapter(CoinTickerConfig::class.java)

    suspend fun clearAllData(widgetId: Int) {
        keyValueStore.withTransaction {
            keyValueStore.delete(displayDataKey(widgetId))
            keyValueStore.delete(configKey(widgetId))
            widgetRefreshEventInterceptor.deleteHash(widgetId)
        }
    }

    suspend fun clearDisplayData(widgetId: Int) {
        keyValueStore.delete(displayDataKey(widgetId))
    }

    suspend fun getDisplayData(widgetId: Int): CoinTickerDisplayData? {
        val key = displayDataKey(widgetId)
        val data = keyValueStore.getString(key) ?: return null
        return displayAdapter.fromJson(data)!!
    }

    suspend fun setDisplayData(widgetId: Int, data: CoinTickerDisplayData) {
        val key = displayDataKey(widgetId)
        val jsonData = displayAdapter.toJson(data)
        keyValueStore.setString(key, jsonData)
    }

    fun getDisplayDataStream(widgetId: Int): Flow<CoinTickerDisplayData> {
        val key = displayDataKey(widgetId)
        return keyValueStore.getStringFlow(key)
            .filterNotNull()
            .map {
                displayAdapter.fromJson(it)!!
            }
    }

    private fun displayDataKey(widgetId: Int): String {
        return "ticker_widget_display_data_$widgetId"
    }

    private fun configKey(widgetId: Int): String {
        return "ticker_widget_config_$widgetId"
    }

    suspend fun setConfig(widgetId: Int, widgetConfig: CoinTickerConfig) {
        val key = configKey(widgetId)
        val data = configAdapter.toJson(widgetConfig)
        keyValueStore.setString(key, data)
        val currentSetting = userSettingRepository.getUserSetting()
        val userSetting = currentSetting.copy(
            currencyCode = widgetConfig.currency,
            refreshInterval = widgetConfig.refreshInterval,
            refreshIntervalUnit = widgetConfig.refreshIntervalUnit,
            amountDecimals = widgetConfig.numberOfAmountDecimal,
            roundToMillion = widgetConfig.roundToMillion,
            showCurrencySymbol = widgetConfig.showCurrencySymbol,
            showThousandsSeparator = widgetConfig.showThousandsSeparator,
            numberOfChangePercentDecimal = widgetConfig.numberOfChangePercentDecimal,
            sizeAdjustment = widgetConfig.sizeAdjustment,
        )
        userSettingRepository.setUserSetting(userSetting)
    }

    suspend fun getConfig(
        widgetId: Int,
        returnDefaultConfigIfMissing: Boolean = true,
        callSite: String? = null
    ): CoinTickerConfig? {
        val key = configKey(widgetId)
        val data = keyValueStore.getString(key)
        if (data == null && returnDefaultConfigIfMissing) {
            return if (isWidgetDeleted(widgetId)) {
                null
            } else {
                if (callSite != null) {
                    tracker.logKeyParamsEvent(
                        "widget_refresh_exception",
                        mapOf(
                            "reason" to "missing_coin_ticker_config",
                            "call_site" to callSite
                        )
                    )
                }
                val defaultConfig = getDefaultTickerWidgetConfig.invoke(widgetId)
                setConfig(widgetId, defaultConfig)
                defaultConfig
            }
        }
        if (data == null) {
            return null
        }
        return configAdapter.fromJson(data)
    }

    fun isWidgetDeleted(widgetId: Int): Boolean {
        val widgetIds = CoinTickerLayout.values().getWidgetIds(context)
        if (widgetId in widgetIds) {
            return false
        }
        return true
    }

    fun getConfigStream(widgetId: Int): Flow<CoinTickerConfig> {
        val key = configKey(widgetId)
        return keyValueStore.getStringFlow(key)
            .filterNotNull()
            .map { configAdapter.fromJson(it)!! }
    }
}
