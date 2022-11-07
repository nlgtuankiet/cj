package com.rainyseason.cj.tracking

import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.widget.OncePerDayEventInterceptor
import com.rainyseason.cj.widget.WidgetRefreshFakeAmountInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface Event

interface Tracker {
    fun log(event: Event): Job
}

interface SyncTracker {
    fun log(event: Event)
}

interface EventInterceptor {
    suspend fun intercept(event: Event, process: suspend (Event) -> Unit)
}

data class KeyParamsEvent(
    val key: String,
    val params: Map<String, Any?>,
) : Event

fun Tracker.logKeyParamsEvent(
    key: String,
    params: Map<String, Any?> = emptyMap(),
): Job {
    val event = KeyParamsEvent(key, params)
    return log(event = event)
}

fun Tracker.logScreenEnter(
    name: String,
    params: Map<String, Any?> = emptyMap()
) {
    val event = KeyParamsEvent(
        "screen_enter",
        mapOf("name" to name) + params
    )
    log(event = event)
}

fun Tracker.logClick(
    screenName: String,
    target: String,
    params: Map<String, Any?> = emptyMap(),
) {
    val finalParams = mapOf(
        "screen_name" to screenName,
        "target" to target
    ) + params
    val event = KeyParamsEvent("click", finalParams)
    log(event = event)
}

// call site

@Singleton
class AppTracker @Inject constructor(
    private val firebaseTracker: FirebaseTracker,
    private val amplitudeTracker: AmplitudeTracker,
    private val debugTracker: DebugTracker,
    widgetRefreshFakeAmountInterceptor: WidgetRefreshFakeAmountInterceptor,
    oncePerDayEventInterceptor: OncePerDayEventInterceptor,
) : Tracker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val trackers: List<SyncTracker> by lazy {
        val list = mutableListOf<SyncTracker>()
        list.add(firebaseTracker)
        list.add(amplitudeTracker)
        if (BuildConfig.DEBUG) {
            list.add(debugTracker)
        }

        list.toList()
    }

    private val interceptors: List<EventInterceptor> = listOf(
        widgetRefreshFakeAmountInterceptor,
        oncePerDayEventInterceptor,
    )

    override fun log(event: Event): Job {
        return scope.launch {
            intercept(event, 0)
        }
    }

    /**
     * Can handle up to about 100 interceptor, if we have more than 100 interceptor then we need to
     * remove this recursion implementation with something else
     */
    private suspend fun intercept(event: Event, interceptorIndex: Int) {
        if (interceptorIndex >= interceptors.size) {
            trackers.forEach { tracker ->
                tracker.log(event)
            }
        } else {
            interceptors[interceptorIndex].intercept(event) { newEvent ->
                intercept(newEvent, interceptorIndex + 1)
            }
        }
    }
}
