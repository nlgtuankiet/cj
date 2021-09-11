package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.data.local.CoinTickerRepository

class CoinTickerProviderDefault : CoinTickerProvider()
class CoinTickerProviderGraph : CoinTickerProvider()
class CoinTickerProviderCoin360 : CoinTickerProvider()

abstract class CoinTickerProvider : AppWidgetProvider() {

    lateinit var coinTickerHandler: CoinTickerHandler

    lateinit var coinTickerRepository: CoinTickerRepository

    lateinit var appWidgetManager: AppWidgetManager

    override fun onReceive(context: Context, intent: Intent) {
        coinTickerHandler = context.coreComponent.coinTickerHandler
        coinTickerRepository = context.coreComponent.coinTickerRepository
        appWidgetManager = context.coreComponent.appWidgetManager
        val action = intent.action
        if (action == CoinTickerConfig.Action.SWITCH_ACTION) {
            goBackground {
                coinTickerHandler.switchPriceAndMarketCap(
                    widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
                )
            }
        }


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