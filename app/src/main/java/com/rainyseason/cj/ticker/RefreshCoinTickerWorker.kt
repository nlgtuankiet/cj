package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.Context
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkManager
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
    private val userSettingRepository: UserSettingRepository,
    private val coinTickerRepository: CoinTickerRepository,
    private val appWidgetManager: AppWidgetManager,
    private val render: TickerWidgerRender,
    private val workManager: WorkManager,
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
        val userCurrency = userSettingRepository.getCurrency()
        val config = coinTickerRepository.getConfig(widgetId)
        if (config == null) {
            Timber.d("missing widget config for id $widgetId")
            // TODO remove this work?
            // TODO launch intent to config the widget?
            return
        }
        val oldDisplayData: TickerWidgetDisplayData = coinTickerRepository.getDisplayData(widgetId)
            ?: throw IllegalStateException("missing display data")

        val loadingView = RemoteViews(appContext.packageName, render.selectLayout(config))
        val loadingParams = TickerWidgetRenderParams(
            userCurrency = userCurrency,
            config = config,
            data = oldDisplayData,
            showLoading = true,
            clickToUpdate = false,
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
            val oldParams = TickerWidgetRenderParams(
                userCurrency = userCurrency,
                config = config,
                data = oldDisplayData,
                showLoading = true,
                clickToUpdate = true,
            )
            render.render(
                view = loadingView,
                params = oldParams,
            )
            appWidgetManager.updateAppWidget(widgetId, oldView)
            return
        }

        val newDisplayData = TickerWidgetDisplayData(
            iconUrl = coinDetail.image.large,
            symbol = coinDetail.symbol,
            name = coinDetail.name,
            price = coinDetail.marketData.currentPrice[userCurrency.id]!!,
            change24hPercent = coinDetail.marketData.priceChangePercentage24h,
            change7dPercent = coinDetail.marketData.priceChangePercentage24h,
            change14dPercent = coinDetail.marketData.priceChangePercentage14d,
        )
        coinTickerRepository.setDisplayData(widgetId = widgetId, data = newDisplayData)
        val newView = RemoteViews(appContext.packageName, render.selectLayout(config))
        val newParams = TickerWidgetRenderParams(
            userCurrency = userCurrency,
            config = config,
            data = newDisplayData,
            showLoading = false,
            clickToUpdate = true,
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