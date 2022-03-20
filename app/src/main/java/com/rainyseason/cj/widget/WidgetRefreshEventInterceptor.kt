package com.rainyseason.cj.widget

import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.tracking.Event
import com.rainyseason.cj.tracking.EventInterceptor
import com.rainyseason.cj.tracking.EventName
import com.rainyseason.cj.tracking.EventParamKey
import com.rainyseason.cj.tracking.KeyParamsEvent
import org.threeten.bp.LocalDate
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRefreshEventInterceptor @Inject constructor(
    private val keyValueStore: KeyValueStore,
) : EventInterceptor {

    /**
     * Track event when any of these properties changed.
     */
    data class HashData(
        val params: Map<String, Any?>,
        val localDate: LocalDate,
    )

    override suspend fun intercept(event: Event, process: suspend (Event) -> Unit) {
        if (event is KeyParamsEvent && event.key == EventName.WIDGET_REFRESH) {
            val widgetId = event.params[EventParamKey.WIDGET_ID] as? Int ?: return
            val cacheKey = hashKey(widgetId)
            val previousHash = keyValueStore.getLong(cacheKey)

            val currentHash = HashData(
                params = event.params,
                localDate = LocalDate.now(),
            ).hashCode().toLong()

            if (currentHash != previousHash) {
                keyValueStore.setLong(cacheKey, currentHash)
                process(event)
            } else {
                Timber.d(
                    "Skip track ${EventName.WIDGET_REFRESH} for " +
                        "widget_id: $widgetId, params: ${event.params}"
                )
            }
        } else {
            process(event)
        }
    }

    suspend fun deleteHash(widgetId: Int) {
        val key = hashKey(widgetId)
        keyValueStore.delete(key)
    }

    private fun hashKey(widgetId: Int): String {
        return "refresh_widget_event_hash_$widgetId"
    }
}
