package com.rainyseason.cj.widget

import com.rainyseason.cj.tracking.Event
import com.rainyseason.cj.tracking.EventInterceptor
import com.rainyseason.cj.tracking.KeyParamsEvent
import javax.inject.Inject

/**
 * Replace amount with fake amount
 */
class WidgetRefreshFakeAmountInterceptor @Inject constructor() : EventInterceptor {
    override suspend fun intercept(event: Event, process: suspend (Event) -> Unit) {
        if (event is KeyParamsEvent &&
            (event.key == "widget_refresh" || event.key == "widget_save") &&
            event.params["amount"] != 1.0
        ) {
            val newEvent = event.copy(params = event.params + mapOf("amount" to 420.0))
            process.invoke(newEvent)
        } else {
            process.invoke(event)
        }
    }
}
