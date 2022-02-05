package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.rainyseason.cj.common.model.getWidgetIds
import com.rainyseason.cj.data.local.CoinTickerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
            getWorkName(widgetId),
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        ).await()
    }

    fun getWorkName(widgetId: Int): String {
        return "refresh_$widgetId"
    }

    suspend fun checkWidgetDeleted(widgetId: Int): Boolean {
        val widgetIds = CoinTickerLayout.values().getWidgetIds(context)
        if (widgetId in widgetIds) {
            return false
        }
        withContext(Dispatchers.IO) {
            onDelete(widgetId)
        }
        return true
    }

    suspend fun onDelete(widgetId: Int) {
        coroutineScope {
            launch(NonCancellable) {
                coinTickerRepository.clearAllData(widgetId = widgetId)
            }
            launch(NonCancellable) {
                removeRefreshWork(widgetId = widgetId)
            }
            renderer.removeNotification(widgetId)
        }
    }

    private suspend fun removeRefreshWork(widgetId: Int) {
        workManager.cancelUniqueWork(getWorkName(widgetId)).await()
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
        renderer.render(params)
    }
}
