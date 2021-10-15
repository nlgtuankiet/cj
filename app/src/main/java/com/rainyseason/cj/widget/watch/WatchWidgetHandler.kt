package com.rainyseason.cj.widget.watch

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchWidgetHandler @Inject constructor(

) {
    suspend fun enqueueRefreshWidget(widgetId: Int, config: WatchConfig) {
    }
}