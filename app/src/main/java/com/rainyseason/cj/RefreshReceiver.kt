package com.rainyseason.cj

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.ticker.CoinTickerProvider

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
        val tickerComponent = ComponentName(context, CoinTickerProvider::class.java)
        val widgetIds = widgetManager.getAppWidgetIds(tickerComponent)
        widgetIds.forEach {
            tickerHandler.enqueueRefreshWidget(it)
        }
    }
}