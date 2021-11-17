package com.rainyseason.cj

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.widget.watch.WatchWidgetLayout
import timber.log.Timber

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
        // refresh add ticker widget
        val widgetManager = context.coreComponent.appWidgetManager
        val tickerHandler = context.coreComponent.coinTickerHandler

        listOf(
            *CoinTickerConfig.Layout.clazzToLayout.keys.toTypedArray(),
            *WatchWidgetLayout.values().map { it.providerName }.toTypedArray()
        ).flatMap { clazz ->
            val tickerComponent = ComponentName(context, clazz)
            widgetManager.getAppWidgetIds(tickerComponent).toList().also {
                Timber.d("refresh widget after boot $tickerComponent $it   ")
            }
        }.forEach { widgetId ->
            tickerHandler.enqueueRefreshWidget(widgetId)
        }
    }
}
