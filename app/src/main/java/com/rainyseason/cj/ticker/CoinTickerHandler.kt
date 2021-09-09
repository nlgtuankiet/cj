package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.rainyseason.cj.data.local.CoinTickerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinTickerHandler @Inject constructor(
    private val workManager: WorkManager,
    private val coinTickerRepository: CoinTickerRepository,
) {
    suspend fun enqueueRefreshWidget(widgetId: Int, config: CoinTickerConfig? = null) {
        val latestConfig = config ?: coinTickerRepository.getConfig(widgetId = widgetId) ?: return
        val request = PeriodicWorkRequestBuilder<RefreshCoinTickerWorker>(
            repeatInterval = latestConfig.refreshInterval,
            repeatIntervalTimeUnit = latestConfig.refreshIntervalUnit
        )
            .setInputData(
                Data.Builder().putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId).build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            "refresh_${widgetId}",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
}