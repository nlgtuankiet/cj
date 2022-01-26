package com.rainyseason.cj.common.model

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

interface WidgetLayout {
    val providerName: String
}

fun Array<out WidgetLayout>.getWidgetIds(context: Context): List<Int> {
    val manager = AppWidgetManager.getInstance(context)
    return flatMap {
        val component = ComponentName(context, it.providerName)
        manager.getAppWidgetIds(component).toList()
    }.filter { it != 0 }
}
