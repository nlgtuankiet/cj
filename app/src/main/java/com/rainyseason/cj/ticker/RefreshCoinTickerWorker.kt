package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.local.CoinTickerRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Refresh
 */
class RefreshCoinTickerWorker @AssistedInject constructor(
    @Assisted val appContext: Context,
    @Assisted val params: WorkerParameters,
    private val coinGeckoService: CoinGeckoService,
    private val coinTickerRepository: CoinTickerRepository,
    private val userSettingRepository: UserSettingRepository,
    private val appWidgetManager: AppWidgetManager,
    private val render: TickerWidgerRender,
) : CoroutineWorker(appContext = appContext, params = params) {

    override suspend fun doWork(): Result {
        val widgetId = params.inputData.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            throw IllegalArgumentException("invalid id")
        }

        updateWidget(widgetId)

        return Result.success()
    }


    /**
     * TODO when user clear app data, the config is missing, so we just return?
     */
    private suspend fun updateWidget(widgetId: Int) {
        val config = coinTickerRepository.getConfig(widgetId)
        if (config == null) {
            Timber.d("missing widget config for id $widgetId")
            // TODO remove this work?
            // TODO launch intent to config the widget?
            return
        }
        val oldDisplayData: CoinTickerDisplayData = coinTickerRepository.getDisplayData(widgetId)
            ?: throw IllegalStateException("missing display data")

        val configCurrency = config.currency
        val userCurrency = userSettingRepository.getCurrencyCode()
        val currencyCode = configCurrency ?: userCurrency
        val loadingView = RemoteViews(appContext.packageName, render.selectLayout(config))
        val loadingParams = CoinTickerRenderParams(
            config = config,
            data = oldDisplayData,
            showLoading = true,
            userCurrency = userCurrency,
        )
        render.render(
            view = loadingView,
            params = loadingParams,
        )
        appWidgetManager.updateAppWidget(widgetId, loadingView)

        val coinDetail: CoinDetailResponse
        try {
            coinDetail = coinGeckoService.getCoinDetail(config.coinId)
        } catch (ex: Exception) {
            // show error ui? toast?
            val oldView = RemoteViews(appContext.packageName, render.selectLayout(config))
            val oldParams = CoinTickerRenderParams(
                config = config,
                data = oldDisplayData,
                showLoading = true,
                userCurrency = userCurrency,
            )
            render.render(
                view = loadingView,
                params = oldParams,
            )
            appWidgetManager.updateAppWidget(widgetId, oldView)
            return
        }

        val graphResponse = coinGeckoService.getMarketChart(
            id = config.coinId,
            vsCurrency = currencyCode,
            day = when (config.changeInterval) {
                ChangeInterval._24H -> 1
                ChangeInterval._7D -> 7
                ChangeInterval._14D -> 14
                ChangeInterval._30D -> 30
                ChangeInterval._1Y -> 365
                else -> error("Unknown ${config.changeInterval}")
            }
        )

        val newDisplayData = CoinTickerDisplayData.create(
            config = config,
            coinDetail = coinDetail,
            marketChartResponse = mapOf(config.changeInterval to graphResponse),
            userCurrency = userCurrency,
        )

        coinTickerRepository.setDisplayData(widgetId = widgetId, data = newDisplayData)
        val newView = RemoteViews(appContext.packageName, render.selectLayout(config))
        val newParams = CoinTickerRenderParams(
            config = config,
            data = newDisplayData,
            showLoading = false,
            userCurrency = userCurrency,
        )
        render.render(
            view = newView,
            params = newParams,
        )
        appWidgetManager.updateAppWidget(widgetId, newView)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            appContext: Context,
            params: WorkerParameters,
        ): RefreshCoinTickerWorker
    }
}