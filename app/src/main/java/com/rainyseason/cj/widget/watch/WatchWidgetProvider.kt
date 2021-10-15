package com.rainyseason.cj.widget.watch

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

class WatchWidget4x2Provider : WatchWidgetProvider()

class WatchWidget4x4Provider : WatchWidgetProvider()

abstract class WatchWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}