package com.rainyseason.cj.widget.watch

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground

class WatchWidget4x2Provider : WatchWidgetProvider()

class WatchWidget4x4Provider : WatchWidgetProvider()

abstract class WatchWidgetProvider : AppWidgetProvider() {
    private lateinit var handler: WatchWidgetHandler
    override fun onReceive(context: Context, intent: Intent) {
        handler = context.coreComponent.watchWidgetHandler
        super.onReceive(context, intent)
        if (intent.action == WatchClickAction.NAME) {
            goBackground {
                handler.handleClickAction(context, intent)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        goBackground {
            appWidgetIds.forEach { widgetId ->
                handler.enqueueRefreshWidget(widgetId)
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        goBackground {
            handler.rerender(appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        goBackground {
            appWidgetIds.forEach { widgetId ->
                handler.onWidgetDelete(widgetId)
            }
        }
    }
}
