package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.rainyseason.cj.R
import timber.log.Timber

class CoinTickerProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("$intent")
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Timber.d("onUpdate")
        appWidgetIds.forEach { widgetId ->
            val view = RemoteViews(context.packageName, R.layout.widget_coin_ticker)
            appWidgetManager.updateAppWidget(widgetId, view)
        }
    }
}