package com.rainyseason.cj.widget

import com.rainyseason.cj.BuildConfig
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

/**
 * Event in [eventKeysToInclude] will be tracked once per day or its parameter changed
 */
@Singleton
class OncePerDayEventInterceptor @Inject constructor(
    private val keyValueStore: KeyValueStore,
) : EventInterceptor {

    private val eventKeysToInclude = arrayOf(
        EventName.WIDGET_REFRESH,
        EventName.WIDGET_REFRESH_FAIL,
    )

    /**
     * Track event when any of these properties changed.
     */
    data class HashData(
        val params: Map<String, Any?>,
        val localDate: LocalDate,
    )

    override suspend fun intercept(event: Event, process: suspend (Event) -> Unit) {
        if (event is KeyParamsEvent && event.key in eventKeysToInclude) {
            val widgetId = event.params[EventParamKey.WIDGET_ID] as? Int
            if (BuildConfig.DEBUG && widgetId == null) {
                throw IllegalStateException(
                    "Event ${event.key} much have ${EventParamKey.WIDGET_ID} param"
                )
            }
            if (widgetId == null) {
                return
            }

            val hashKey = hashKey(event.key, widgetId)
            val previousHash = keyValueStore.getLong(hashKey)

            val currentHash = HashData(
                params = event.params,
                localDate = LocalDate.now(),
            ).hashCode().toLong()

            if (currentHash != previousHash) {
                keyValueStore.setLong(hashKey, currentHash)
                process(event)
            } else {
                Timber.d(
                    "Skip track ${event.key} for " +
                        "widget_id: $widgetId, params: ${event.params}"
                )
            }
        } else {
            process(event)
        }
    }

    suspend fun deleteHash(widgetId: Int) {
        eventKeysToInclude.forEach { key ->
            val hashKey = hashKey(key, widgetId)
            keyValueStore.delete(hashKey)
        }
    }

    private fun hashKey(eventKey: String, widgetId: Int): String {
        return "event_hash_${eventKey}_$widgetId"
    }
}
