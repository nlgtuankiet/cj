package com.rainyseason.cj.tracking

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.widget.OncePerDayEventInterceptor
import com.rainyseason.cj.widget.WidgetRefreshFakeAmountInterceptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
class FirebaseTracker @Inject constructor(
    private val firebaseAnalytics: Provider<FirebaseAnalytics>,
    private val firebaseCrashlytics: Provider<FirebaseCrashlytics>,
) : SyncTracker {

    override fun log(event: Event) {
        when {
            isTickerRefreshEvent(event) -> logTickerWidgetRefresh(event)
            event is KeyParamsEvent -> logKeyParamsEvent(event)
        }
    }

    private val ticketWidgetRefreshWhiteList = setOf(
        EventParamKey.WIDGET_ID,
        "coin_id",
        "backend",
        "refresh_interval_seconds",
        "theme",
        "change_interval",
        "layout",
        "click_action",
        "show_currency_symbol",
        "currency",
        "round_to_million",
        "background_transparency",
        "hide_decimal_on_large_price",
        "show_notification",
        "full_size",
        "amount",
        "network",
        "dex",
        "reverse_pair",
        "display_symbol",
        "display_name",
        "reverse_positive_color",
        "calling_package",
        "has_app_widget_sizes",
    )

    /**
     * Firebase Analytic has 25 event params limit so we need to pick and choose params
     */
    private fun logTickerWidgetRefresh(event: KeyParamsEvent) {
        val newEvent = event.copy(
            params = event.params.filter { it.key in ticketWidgetRefreshWhiteList }
        )
        logKeyParamsEvent(newEvent)
    }

    @OptIn(ExperimentalContracts::class)
    private fun isTickerRefreshEvent(event: Event): Boolean {
        contract {
            returns(true) implies (event is KeyParamsEvent)
        }
        return event is KeyParamsEvent &&
            (event.key == "widget_refresh" || event.key == "widget_save") &&
            event.params.containsKey("backend")
    }

    private fun logKeyParamsEvent(event: KeyParamsEvent) {
        val bundle = Bundle()
        event.params.forEach { (key, value) ->
            when (value) {
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is String -> bundle.putString(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                else -> recordUnknownParamType(firebaseCrashlytics, event, key)
            }
        }
        if (BuildConfig.DEBUG) {
            Timber.d("Log ${event.key} ${event.params}")
        }
        firebaseAnalytics.get().logEvent(event.key, bundle)
    }
}

class DebugTracker @Inject constructor() : SyncTracker {
    override fun log(event: Event) {
        when (event) {
            is KeyParamsEvent -> logKeyParamsEvent(event)
        }
    }

    private fun logKeyParamsEvent(event: KeyParamsEvent) {
        val params = event.params.toList().sortedBy { it.first }
        Timber.d("""Log "${event.key}" values $params""")
    }
}

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
