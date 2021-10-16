package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.common.WatchListRepository
import com.rainyseason.cj.common.asNoNetworkException
import com.rainyseason.cj.common.changePercent
import com.rainyseason.cj.common.isInBatteryOptimize
import com.rainyseason.cj.common.model.asDayString
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import com.rainyseason.cj.widget.watch.WatchDisplayData
import com.rainyseason.cj.widget.watch.WatchDisplayEntry
import com.rainyseason.cj.widget.watch.WatchDisplayEntryContent
import com.rainyseason.cj.widget.watch.WatchWidget4x2Provider
import com.rainyseason.cj.widget.watch.WatchWidget4x4Provider
import com.rainyseason.cj.widget.watch.WatchWidgetHandler
import com.rainyseason.cj.widget.watch.WatchWidgetRender
import com.rainyseason.cj.widget.watch.WatchWidgetRenderParams
import com.rainyseason.cj.widget.watch.WatchWidgetRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import timber.log.Timber

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
        val widgetIds = listOf(
            WatchWidget4x2Provider::class.java,
            WatchWidget4x4Provider::class.java,
        ).map { appWidgetManager.getAppWidgetIds(ComponentName(appContext, it)).toList() }
            .flatten()

        if (widgetId !in widgetIds) {
            handler.removeRefreshWork(widgetId)
        }

        if (appContext.isInBatteryOptimize()) {
            tracker.logKeyParamsEvent(
                "widget_refresh_fail",
                mapOf(
                    "reason" to "in_battery_optimize"
                )
            )
            return Result.retry()
        }

        try {
            coroutineScope {
                updateWidget(widgetId)
            }
        } catch (ex: Throwable) {
            tracker.logKeyParamsEvent(
                "widget_refresh_fail",
                mapOf(
                    "reason" to "unknown",
                    "message" to ex.message
                )
            )
            val networkException = ex.asNoNetworkException(appContext)
            firebaseCrashlytics.recordException(networkException ?: ex)
            if (networkException != null) {
                return Result.retry()
            }
        }

        return Result.success()
    }

    /**
     * TODO fix coroutine scope warning
     */
    private suspend fun CoroutineScope.updateWidget(widgetId: Int) {
        val config = watchWidgetRepository.getConfig(widgetId)
        if (config == null) {
            Timber.d("missing widget config for id $widgetId")
            // TODO remove this work?
            // TODO launch intent to config the widget?
            return
        }

        tracker.logKeyParamsEvent(
            key = "widget_refresh",
            params = config.getTrackingParams(),
        )

        val oldDisplayData: WatchDisplayData = watchWidgetRepository.getDisplayData(widgetId)
            ?: throw IllegalStateException("missing display data")

        val configCurrency = config.currency
        val loadingView = RemoteViews(appContext.packageName, config.layout.layout)
        val loadingParams = WatchWidgetRenderParams(
            config = config,
            data = oldDisplayData,
            showLoading = true,
        )
        render.render(
            remoteView = loadingView,
            inputParams = loadingParams,
        )
        appWidgetManager.updateAppWidget(widgetId, loadingView)

        try {
            val watchList = watchListRepository.getWatchList().first()
                .take(config.layout.entryLimit)
            val entries = watchList.map { coinId ->
                async {
                    val coinDetail = coinGeckoService.getCoinDetail(coinId)
                    val coinMarket = coinGeckoService.getMarketChart(
                        coinId,
                        configCurrency,
                        config.interval.asDayString()!!
                    )
                    val priceChart =
                        coinMarket.prices.filter { it.size == 2 }.takeIf { it.size >= 2 }
                    WatchDisplayEntry(
                        coinId = coinId,
                        content = WatchDisplayEntryContent(
                            symbol = coinDetail.symbol,
                            name = coinDetail.name,
                            graph = priceChart,
                            price = coinDetail.marketData.currentPrice[configCurrency]!!,
                            changePercent = priceChart?.changePercent()
                        )
                    )
                }
            }.awaitAll()
            val data = WatchDisplayData(entries)
            val newView = RemoteViews(appContext.packageName, config.layout.layout)
            val newParams = WatchWidgetRenderParams(
                config = config,
                data = data,
                showLoading = false,
                isPreview = false
            )
            render.render(
                remoteView = newView,
                inputParams = newParams
            )
            appWidgetManager.updateAppWidget(config.widgetId, newView)
        } catch (ex: Exception) {
            // show error ui? toast?
            val errorView = RemoteViews(appContext.packageName, config.layout.layout)
            val oldParams = WatchWidgetRenderParams(
                config = config,
                data = oldDisplayData,
                showLoading = false,
                isPreview = false
            )
            render.render(
                remoteView = errorView,
                inputParams = oldParams,
            )
            appWidgetManager.updateAppWidget(widgetId, errorView)
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