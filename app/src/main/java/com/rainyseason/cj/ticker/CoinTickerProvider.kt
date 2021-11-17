package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.data.local.CoinTickerRepository

class CoinTickerProviderDefault : CoinTickerProvider()
class CoinTickerProviderGraph : CoinTickerProvider()
class CoinTickerProviderCoin360 : CoinTickerProvider()
class CoinTickerProviderCoin360Mini : CoinTickerProvider()
class CoinTickerProviderMini : CoinTickerProvider()
class CoinTickerProviderNano : CoinTickerProvider()
class CoinTickerProviderIconSmall : CoinTickerProvider()

abstract class CoinTickerProvider : AppWidgetProvider() {

    private lateinit var coinTickerHandler: CoinTickerHandler

    private lateinit var coinTickerRepository: CoinTickerRepository

    private lateinit var appWidgetManager: AppWidgetManager

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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        goBackground {
            coinTickerHandler.rerender(appWidgetId)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
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
                coinTickerHandler.removeRefreshWork(widgetId = it)
            }
        }
    }
}
