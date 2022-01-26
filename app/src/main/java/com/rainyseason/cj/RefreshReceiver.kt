package com.rainyseason.cj

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.common.model.getWidgetIds
import com.rainyseason.cj.ticker.CoinTickerLayout
import com.rainyseason.cj.widget.watch.WatchWidgetLayout

class BootRefreshReceiver : RefreshReceiver()
class PackageReplaceRefreshReceiver : RefreshReceiver()

abstract class RefreshReceiver : BroadcastReceiver() {
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
            handle(context)
        }
    }

    private suspend fun handle(context: Context) {
        val tickerHandler = context.coreComponent.coinTickerHandler
        val watchHandler = context.coreComponent.watchWidgetHandler

        CoinTickerLayout.values().getWidgetIds(context)
            .forEach { widgetId ->
                tickerHandler.rerender(widgetId)
                tickerHandler.enqueueRefreshWidget(widgetId)
            }

        WatchWidgetLayout.values().getWidgetIds(context)
            .forEach { widgetId ->
                watchHandler.rerender(widgetId)
                watchHandler.enqueueRefreshWidget(widgetId)
            }
    }
}
