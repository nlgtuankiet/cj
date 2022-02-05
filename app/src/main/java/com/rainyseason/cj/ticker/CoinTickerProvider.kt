package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.data.local.CoinTickerRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class CoinTickerProviderDefault : CoinTickerProvider()
class CoinTickerProviderGraph : CoinTickerProvider()
class CoinTickerProviderCoin360 : CoinTickerProvider()
class CoinTickerProviderCoin360Mini : CoinTickerProvider()
class CoinTickerProviderMini : CoinTickerProvider()
class CoinTickerProviderNano : CoinTickerProvider()
class CoinTickerProviderIconSmall : CoinTickerProvider()

abstract class CoinTickerProvider : AppWidgetProvider() {

    companion object {
        val PROVIDERS = listOf(
            CoinTickerProviderDefault::class.java,
            CoinTickerProviderGraph::class.java,
            CoinTickerProviderCoin360::class.java,
            CoinTickerProviderCoin360Mini::class.java,
            CoinTickerProviderMini::class.java,
            CoinTickerProviderNano::class.java,
            CoinTickerProviderIconSmall::class.java,
        )
    }

    private lateinit var coinTickerHandler: CoinTickerHandler

    private lateinit var coinTickerRepository: CoinTickerRepository

    private lateinit var appWidgetManager: AppWidgetManager

    override fun onReceive(context: Context, intent: Intent) {
        coinTickerHandler = context.coreComponent.coinTickerHandler
        coinTickerRepository = context.coreComponent.coinTickerRepository
        appWidgetManager = context.coreComponent.appWidgetManager
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
                val deleted = coinTickerHandler.checkWidgetDeleted(it)
                if (!deleted) {
                    coinTickerHandler.enqueueRefreshWidget(it)
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        goBackground {
            coroutineScope {
                appWidgetIds.forEach {
                    launch {
                        coinTickerHandler.onDelete(widgetId = it)
                    }
                }
            }
        }
    }
}
