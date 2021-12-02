package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.exception.logFallbackPrice
import com.rainyseason.cj.common.hasValidNetworkConnection
import com.rainyseason.cj.common.isInBatteryOptimize
import com.rainyseason.cj.common.model.TimeInterval
import com.rainyseason.cj.common.model.asDayString
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.currentPrice
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * Refresh
 */
class RefreshCoinTickerWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted val params: WorkerParameters,
    private val coinGeckoService: CoinGeckoService,
    private val coinTickerRepository: CoinTickerRepository,
    private val appWidgetManager: AppWidgetManager,
    private val handler: CoinTickerHandler,
    private val render: TickerWidgetRenderer,
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
        val widgetIds = CoinTickerConfig.Layout
            .clazzToLayout.keys
            .map { appWidgetManager.getAppWidgetIds(ComponentName(appContext, it)).toList() }
            .flatten()

        if (widgetId !in widgetIds) {
            handler.removeRefreshWork(widgetId)
            return Result.success()
        }

        if (appContext.isInBatteryOptimize()) {
            tracker.logKeyParamsEvent(
                "widget_refresh_fail",
                mapOf("reason" to "in_battery_optimize")
            )
            return Result.success()
        }

        if (!appContext.hasValidNetworkConnection()) {
            tracker.logKeyParamsEvent(
                "widget_refresh_fail",
                mapOf("reason" to "no_network")
            )
            return Result.success()
        }

        try {
            updateWidget(widgetId)
        } catch (ex: Throwable) {
            tracker.logKeyParamsEvent(
                "widget_refresh_fail",
                mapOf(
                    "reason" to "unknown",
                    "message" to ex.message
                )
            )
            firebaseCrashlytics.recordException(ex)
            if (BuildConfig.DEBUG) {
                throw ex
            }
        }

        return Result.success()
    }

    private suspend fun updateWidget(widgetId: Int) {
        val config = coinTickerRepository.getConfig(widgetId)
        if (config == null) {
            handler.removeRefreshWork(widgetId)
            return
        }

        tracker.logKeyParamsEvent(
            key = "widget_refresh",
            params = config.getTrackingParams(),
        )

        val configCurrency = config.currency
        val oldDisplayData = coinTickerRepository.getDisplayData(widgetId)

        if (oldDisplayData != null) {
            val loadingView = RemoteViews(appContext.packageName, render.selectLayout(config))
            val loadingParams = CoinTickerRenderParams(
                config = config,
                data = oldDisplayData,
                showLoading = true,
            )
            render.render(
                view = loadingView,
                inputParams = loadingParams,
            )
            appWidgetManager.updateAppWidget(widgetId, loadingView)
        } else {
            firebaseCrashlytics.recordException(
                IllegalStateException("missing display data ${config.layout}")
            )
        }

        try {
            val coinDetail = coinGeckoService.getCoinDetail(config.coinId)
            val graphResponse = coinGeckoService.getMarketChart(
                id = config.coinId,
                vsCurrency = configCurrency,
                day = config.changeInterval.asDayString()!!
            )

            val marketPrice = if (config.changeInterval == TimeInterval.I_24H) {
                graphResponse.currentPrice()
            } else {
                coinGeckoService.getMarketChart(
                    id = config.coinId,
                    vsCurrency = configCurrency,
                    day = "1",
                ).currentPrice()
            }

            val price = marketPrice ?: coinDetail.marketData.currentPrice[config.currency]
            if (marketPrice == null) {
                firebaseCrashlytics.logFallbackPrice(config.coinId)
            }
            val newDisplayData = CoinTickerDisplayData.create(
                config = config,
                coinDetail = coinDetail,
                marketChartResponse = mapOf(config.changeInterval to graphResponse),
                price = price
            )

            coinTickerRepository.setDisplayData(widgetId = widgetId, data = newDisplayData)
            val newView = RemoteViews(appContext.packageName, render.selectLayout(config))
            val newParams = CoinTickerRenderParams(
                config = config,
                data = newDisplayData,
                showLoading = false,
            )
            render.render(
                view = newView,
                inputParams = newParams,
            )
            appWidgetManager.updateAppWidget(widgetId, newView)
        } catch (ex: Exception) {
            if (oldDisplayData != null) {
                val errorView = RemoteViews(appContext.packageName, render.selectLayout(config))
                val oldParams = CoinTickerRenderParams(
                    config = config,
                    data = oldDisplayData,
                    showLoading = false,
                    isPreview = false
                )
                render.render(
                    view = errorView,
                    inputParams = oldParams,
                )
                appWidgetManager.updateAppWidget(widgetId, errorView)
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
