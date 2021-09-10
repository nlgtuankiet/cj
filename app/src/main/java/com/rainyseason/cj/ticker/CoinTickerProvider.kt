package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.common.logString
import com.rainyseason.cj.data.local.CoinTickerRepository
import timber.log.Timber

class CoinTickerProviderDefault : CoinTickerProvider()
class CoinTickerProviderGraph : CoinTickerProvider()
class CoinTickerProviderCoin360 : CoinTickerProvider()

abstract class CoinTickerProvider : AppWidgetProvider() {

    lateinit var coinTickerHandler: CoinTickerHandler

    lateinit var coinTickerRepository: CoinTickerRepository

    lateinit var appWidgetManager: AppWidgetManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive: ${intent.logString()}")
        coinTickerHandler = context.coreComponent.coinTickerHandler
        coinTickerRepository = context.coreComponent.coinTickerRepository
        appWidgetManager = context.coreComponent.appWidgetManager
        super.onReceive(context, intent)
    }


    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        goBackground {
            appWidgetIds.forEach {
                coinTickerHandler.enqueueRefreshWidget(it)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        goBackground {
            appWidgetIds.forEach {
                coinTickerRepository.clearAllData(widgetId = it)
            }
        }
    }
}