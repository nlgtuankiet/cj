package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.getTrackingParams
import com.rainyseason.cj.common.hasValidNetworkConnection
import com.rainyseason.cj.common.isInBatteryOptimize
import com.rainyseason.cj.common.model.Watchlist
import com.rainyseason.cj.common.model.getWidgetIds
import com.rainyseason.cj.common.usecase.GetWatchDisplayEntry
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.tracking.EventName
import com.rainyseason.cj.tracking.EventParamKey
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import com.rainyseason.cj.widget.watch.WatchDisplayData
import com.rainyseason.cj.widget.watch.WatchDisplayEntry
import com.rainyseason.cj.widget.watch.WatchDisplayEntryLoadParam
import com.rainyseason.cj.widget.watch.WatchWidgetHandler
import com.rainyseason.cj.widget.watch.WatchWidgetLayout
import com.rainyseason.cj.widget.watch.WatchWidgetRender
import com.rainyseason.cj.widget.watch.WatchWidgetRenderParams
import com.rainyseason.cj.widget.watch.WatchWidgetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Refresh
 */
class RefreshWatchWidgetWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted val params: WorkerParameters,
    private val coinGeckoService: CoinGeckoService,
    private val watchWidgetRepository: WatchWidgetRepository,
    private val appWidgetManager: AppWidgetManager,
    private val handler: WatchWidgetHandler,
    private val render: WatchWidgetRender,
    private val watchListRepository: WatchListRepository,
    private val tracker: Tracker,
    private val firebaseCrashlytics: FirebaseCrashlytics,
    private val getWatchDisplayEntry: GetWatchDisplayEntry,
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
        val widgetIds = WatchWidgetLayout.values().getWidgetIds(appContext)

        if (widgetId !in widgetIds) {
            handler.removeRefreshWork(widgetId)
            return Result.success()
        }

        if (appContext.isInBatteryOptimize()) {
            tracker.logKeyParamsEvent(
                EventName.WIDGET_REFRESH_FAIL,
                mapOf(
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
        val config = watchWidgetRepository.getConfig(widgetId)
        if (config == null) {
            handler.removeRefreshWork(widgetId)
            return
        }

        tracker.logKeyParamsEvent(
            key = "widget_refresh",
            params = config.getTrackingParams() + appWidgetManager.getTrackingParams(widgetId),
        )

        val configCurrency = config.currency
        val oldDisplayData = watchWidgetRepository.getDisplayData(widgetId)

        if (oldDisplayData != null) {
            val loadingParams = WatchWidgetRenderParams(
                config = config,
                data = oldDisplayData,
                showLoading = true,
            )
            render.render(widgetId, loadingParams)
        } else {
            firebaseCrashlytics.recordException(
                IllegalStateException("missing display data ${config.layout}")
            )
        }

        try {
            val watchList = watchListRepository.getWatchlistCollection()
                .list.firstOrNull { it.id == Watchlist.DEFAULT_ID }
                ?.coins.orEmpty()
                .take(
                    if (config.fullSize) {
                        Int.MAX_VALUE
                    } else {
                        config.layout.entryLimit
                    }
                )
            val entries = coroutineScope {
                watchList.map { coin ->
                    async {
                        val data = getWatchDisplayEntry(
                            WatchDisplayEntryLoadParam(
                                coin = coin,
                                currency = configCurrency,
                                changeInterval = config.interval
                            )
                        )
                        WatchDisplayEntry(
                            coinId = coin.id,
                            backend = coin.backend,
                            network = coin.network,
                            dex = coin.dex,
                            content = data
                        )
                    }
                }
            }.awaitAll()
            val data = WatchDisplayData(entries)
            watchWidgetRepository.setDisplayData(widgetId, data)
            val newParams = WatchWidgetRenderParams(
                config = config,
                data = data,
                showLoading = false,
                isPreview = false
            )
            render.render(widgetId, newParams)
        } catch (ex: Exception) {
            if (oldDisplayData != null) {
                val oldParams = WatchWidgetRenderParams(
                    config = config,
                    data = oldDisplayData,
                    showLoading = false,
                    isPreview = false
                )
                render.render(widgetId, oldParams)
            }
            throw ex
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            appContext: Context,
            params: WorkerParameters,
        ): RefreshWatchWidgetWorker
    }
}
