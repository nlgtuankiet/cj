package com.rainyseason.cj

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.ticker.CoinTickerProviderCoin360
import com.rainyseason.cj.ticker.CoinTickerProviderDefault
import com.rainyseason.cj.ticker.CoinTickerProviderGraph

class RefreshReceiver : BroadcastReceiver() {
    private val actions = listOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action !in actions) {
            return
        }
        goBackground {
            handle(context, intent)
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        // refresh add ticker widget
        val widgetManager = context.coreComponent.appWidgetManager
        val tickerHandler = context.coreComponent.coinTickerHandler

        listOf(
            CoinTickerProviderDefault::class.java,
            CoinTickerProviderCoin360::class.java,
            CoinTickerProviderGraph::class.java,
        ).flatMap { clazz ->
            val tickerComponent = ComponentName(context, clazz)
            widgetManager.getAppWidgetIds(tickerComponent).toList()
        }.forEach { widgetId ->
            tickerHandler.enqueueRefreshWidget(widgetId)
        }
    }
}