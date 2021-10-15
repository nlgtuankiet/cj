package com.rainyseason.cj.widget.watch

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.rainyseason.cj.MainActivity
import com.rainyseason.cj.R
import com.rainyseason.cj.common.getWidgetId
import com.rainyseason.cj.ticker.RefreshWatchWidgetWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchWidgetHandler @Inject constructor(
    private val watchWidgetRepository: WatchWidgetRepository,
    private val workManager: WorkManager
) {

    suspend fun enqueueRefreshWidget(widgetId: Int, config: WatchConfig? = null) {
        val latestConfig = config ?: watchWidgetRepository.getConfig(widgetId = widgetId) ?: return
        val request = PeriodicWorkRequestBuilder<RefreshWatchWidgetWorker>(
            repeatInterval = latestConfig.refreshInterval,
            repeatIntervalTimeUnit = latestConfig.refreshIntervalUnit
        )
            .setInputData(
                Data.Builder().putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId).build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5L, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            "refresh_watch_$widgetId",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        ).await()
    }

    suspend fun handleClickAction(context: Context, intent: Intent) {
        val widgetId = intent.extras?.getWidgetId() ?: return
        val config = watchWidgetRepository.getConfig(widgetId) ?: return
        when (config.clickAction) {
            WatchClickAction.OpenWatchlist -> {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(MainActivity.SCREEN_TO_OPEN_EXTRA, R.id.watch_list_screen)
                    }
                )
            }
            WatchClickAction.Refresh -> {
                enqueueRefreshWidget(widgetId)
            }
        }
    }

    suspend fun removeRefreshWork(widgetId: Int) {
        workManager.cancelUniqueWork("refresh_watch_$widgetId").await()
    }
}