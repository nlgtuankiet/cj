package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import android.widget.RemoteViews
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rainyseason.cj.R
import com.rainyseason.cj.data.UserSettingRepository
import com.rainyseason.cj.data.coingecko.CoinDetailResponse
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.local.CoinTickerRepository
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
    private val userSettingRepository: UserSettingRepository,
    private val coinTickerRepository: CoinTickerRepository,
    private val appWidgetManager: AppWidgetManager,
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


    private suspend fun updateWidget(widgetId: Int) {
        val userCurrency = userSettingRepository.getCurrency()
        val config = coinTickerRepository.getConfig(widgetId)
            ?: throw IllegalStateException("Missing config")
        val oldDisplayData: TickerWidgetDisplayData = coinTickerRepository.getDisplayData(widgetId)
            ?: throw IllegalStateException("Missing data")

        val loadingView = RemoteViews(appContext.packageName, R.layout.widget_coin_ticker)
        oldDisplayData.bindTo(loadingView)
        loadingView.setViewVisibility(R.id.loading, View.VISIBLE)
        appWidgetManager.updateAppWidget(widgetId, loadingView)

        val coinDetail: CoinDetailResponse
        try {
            coinDetail = coinGeckoService.getCoinDetail(config.coinId)
        } catch (ex: Exception) {
            // show error ui? toast?
            val oldView = RemoteViews(appContext.packageName, R.layout.widget_coin_ticker)
            oldDisplayData.bindTo(loadingView)
            appWidgetManager.updateAppWidget(widgetId, oldView)
            return
        }

        val newDisplayData = TickerWidgetDisplayData(
            iconUrl = coinDetail.image.large,
            symbol = coinDetail.symbol,
            currentPrice = coinDetail.marketData.currentPrice[userCurrency.id]!!,
            currencySymbol = userCurrency.symbol,
            currencySymbolOnTheLeft = userCurrency.placeOnTheLeft,
            separator = userCurrency.separator,
            priceChangePercentage24h = coinDetail.marketData.priceChangePercentage24h,
            priceChangePercentage7d = coinDetail.marketData.priceChangePercentage7d,
        )
        coinTickerRepository.setDisplayConfig(widgetId = widgetId, data = newDisplayData)
        val newView = RemoteViews(appContext.packageName, R.layout.widget_coin_ticker)
        newDisplayData.bindTo(newView)
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