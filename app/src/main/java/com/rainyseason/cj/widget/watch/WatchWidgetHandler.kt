package com.rainyseason.cj.widget.watch

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.rainyseason.cj.MainActivity
import com.rainyseason.cj.common.getWidgetId
import com.rainyseason.cj.ticker.RefreshWatchWidgetWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchWidgetHandler @Inject constructor(
    private val watchWidgetRepository: WatchWidgetRepository,
    private val workManager: WorkManager,
    private val context: Context,
    private val watchWidgetRender: WatchWidgetRender,
    private val appWidgetManager: AppWidgetManager,
) {

    suspend fun onWidgetDelete(widgetId: Int) {
        watchWidgetRepository.clearAllData(widgetId = widgetId)
        removeRefreshWork(widgetId = widgetId)
    }

    suspend fun enqueueRefreshWidget(widgetId: Int, config: WatchConfig? = null) {
        val latestConfig = config ?: watchWidgetRepository.getConfig(widgetId = widgetId) ?: return
        val request = PeriodicWorkRequestBuilder<RefreshWatchWidgetWorker>(
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
            getWorkName(widgetId),
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        ).await()
    }

    fun getWorkName(widgetId: Int): String {
        return "refresh_watch_$widgetId"
    }

    suspend fun handleClickAction(context: Context, intent: Intent) {
        val widgetId = intent.extras?.getWidgetId() ?: return
        val config = watchWidgetRepository.getConfig(widgetId) ?: return
        when (config.clickAction) {
            WatchClickAction.OpenWatchlist -> {
                context.startActivity(MainActivity.watchListIntent(context))
            }
            WatchClickAction.Refresh -> {
                enqueueRefreshWidget(widgetId)
            }
        }
    }

    suspend fun removeRefreshWork(widgetId: Int) {
        workManager.cancelUniqueWork(getWorkName(widgetId)).await()
    }

    suspend fun rerender(widgetId: Int) {
        val config = watchWidgetRepository.getConfig(widgetId = widgetId) ?: return
        val displayData = watchWidgetRepository.getDisplayData(widgetId = widgetId) ?: return
        val params = WatchWidgetRenderParams(
            config = config,
            data = displayData,
            showLoading = false,
            isPreview = false,
        )
        watchWidgetRender.render(widgetId, params)
    }
}
