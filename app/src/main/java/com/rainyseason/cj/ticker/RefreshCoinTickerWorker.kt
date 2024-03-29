package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.getTrackingParams
import com.rainyseason.cj.common.hasValidNetworkConnection
import com.rainyseason.cj.common.isInBatteryOptimize
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.ticker.usecase.GetDisplayData
import com.rainyseason.cj.tracking.EventName
import com.rainyseason.cj.tracking.EventParamKey
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Refresh
 */
class RefreshCoinTickerWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted val params: WorkerParameters,
    private val coinTickerRepository: CoinTickerRepository,
    private val appWidgetManager: AppWidgetManager,
    private val handler: CoinTickerHandler,
    private val render: TickerWidgetRenderer,
    private val tracker: Tracker,
    private val getDisplayData: GetDisplayData,
    private val firebaseCrashlytics: FirebaseCrashlytics,
) : CoroutineWorker(appContext = appContext, params = params) {

    override suspend fun doWork(): Result {
        val widgetId = params.inputData.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            throw IllegalArgumentException("invalid id")
        }

        // check if widget has been removed
        val deleted = handler.cleanUpIfWidgetDeleted(widgetId)
        if (deleted) {
            return Result.success()
        }

        if (appContext.isInBatteryOptimize()) {
            tracker.logKeyParamsEvent(
                EventName.WIDGET_REFRESH_FAIL,
                mapOf(
                    "reason" to "in_battery_optimize",
                    EventParamKey.WIDGET_ID to widgetId,
                )
            )
            return Result.success()
        }

        if (!appContext.hasValidNetworkConnection()) {
            tracker.logKeyParamsEvent(
                EventName.WIDGET_REFRESH_FAIL,
                mapOf(
                    "reason" to "no_network",
                    EventParamKey.WIDGET_ID to widgetId,
                )
            )
            return Result.success()
        }

        try {
            updateWidget(widgetId)
        } catch (ex: Throwable) {
            if (ex is CancellationException) {
                throw ex
            } else {
                tracker.logKeyParamsEvent(
                    EventName.WIDGET_REFRESH_FAIL,
                    mapOf(
                        "reason" to "unknown",
                        EventParamKey.WIDGET_ID to widgetId,
                    )
                )
                firebaseCrashlytics.recordException(ex)
                if (BuildConfig.DEBUG) {
                    throw ex
                }
            }
        }

        return Result.success()
    }

    private suspend fun updateWidget(widgetId: Int) {
        val config = coinTickerRepository.getConfig(widgetId, callSite = "update_widget")
        if (config == null) {
            handler.onDelete(widgetId)
            return
        }

        tracker.logKeyParamsEvent(
            key = "widget_refresh",
            params = config.getTrackingParams() + appWidgetManager.getTrackingParams(widgetId),
        )

        val oldDisplayData = coinTickerRepository.getDisplayData(widgetId)

        if (oldDisplayData != null) {
            val loadingParams = CoinTickerRenderParams(
                config = config,
                data = oldDisplayData,
                showLoading = true,
            )
            render.render(
                inputParams = loadingParams,
            )
        } else {
            firebaseCrashlytics.recordException(
                IllegalStateException("missing display data ${config.layout}")
            )
        }

        try {
            val newDisplayData = getDisplayData(config.asDataLoadParams())
            coinTickerRepository.setDisplayData(widgetId = widgetId, data = newDisplayData)
            val newParams = CoinTickerRenderParams(
                config = config,
                data = newDisplayData,
                showLoading = false,
            )
            render.render(
                inputParams = newParams,
            )
        } catch (ex: Exception) {
            if (oldDisplayData != null) {
                val oldParams = CoinTickerRenderParams(
                    config = config,
                    data = oldDisplayData,
                    showLoading = false,
                    isPreview = false
                )
                render.render(
                    inputParams = oldParams,
                )
            }
            throw ex
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            appContext: Context,
            params: WorkerParameters,
        ): RefreshCoinTickerWorker
    }
}
