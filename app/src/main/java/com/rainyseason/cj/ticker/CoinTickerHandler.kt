package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.rainyseason.cj.data.local.CoinTickerRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinTickerHandler @Inject constructor(
    private val context: Context,
    private val workManager: WorkManager,
    private val coinTickerRepository: CoinTickerRepository,
    private val renderer: TickerWidgetRenderer,
    private val appWidgetManager: AppWidgetManager,
) {
    suspend fun enqueueRefreshWidget(widgetId: Int, config: CoinTickerConfig? = null) {
        val latestConfig = config ?: coinTickerRepository.getConfig(widgetId = widgetId) ?: return
        Timber.d("enqueueRefreshWidget $widgetId")
        val request = PeriodicWorkRequestBuilder<RefreshCoinTickerWorker>(
            repeatInterval = latestConfig.refreshInterval,
            repeatIntervalTimeUnit = latestConfig.refreshIntervalUnit
        )
            .setInputData(
                Data.Builder().putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId).build()
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            "refresh_$widgetId",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        ).await()
    }

    suspend fun removeRefreshWork(widgetId: Int) {
        workManager.cancelUniqueWork("refresh_$widgetId").await()
    }

    suspend fun rerender(widgetId: Int) {
        val config = coinTickerRepository.getConfig(widgetId = widgetId) ?: return
        val displayData = coinTickerRepository.getDisplayData(widgetId = widgetId) ?: return

        val params = CoinTickerRenderParams(
            config = config,
            data = displayData,
            showLoading = false,
            isPreview = false,
        )
        val view = RemoteViews(context.packageName, renderer.selectLayout(config))
        renderer.render(view, params)
        appWidgetManager.updateAppWidget(widgetId, view)
    }
}
