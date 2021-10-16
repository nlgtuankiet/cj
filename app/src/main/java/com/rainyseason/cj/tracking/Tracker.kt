package com.rainyseason.cj.tracking

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rainyseason.cj.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface Event

interface Tracker {
    fun log(event: Event)
}

class KeyParamsEvent(
    val key: String,
    val params: Map<String, Any?>,
) : Event

fun Tracker.logKeyParamsEvent(
    key: String,
    params: Map<String, Any?> = emptyMap(),
) {
    val event = KeyParamsEvent(key, params)
    log(event = event)
}

fun Tracker.logScreenEnter(name: String) {
    val event = KeyParamsEvent("screen_enter", mapOf("name" to name))
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
    private val firebaseAnalytics: FirebaseAnalytics,
) : Tracker {
    override fun log(event: Event) {
        when (event) {
            is KeyParamsEvent -> logKeyParamsEvent(event)
        }
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
                else -> if (value != null) {
                    error("Unsupport $value for event ${event.key} -> $key")
                }
            }
        }
        firebaseAnalytics.logEvent(event.key, bundle)
    }
}

class DebugTracker @Inject constructor() : Tracker {
    override fun log(event: Event) {
        when (event) {
            is KeyParamsEvent -> logKeyParamsEvent(event)
        }
    }

    private fun logKeyParamsEvent(event: KeyParamsEvent) {
        val params = event.params.toList().sortedBy { it.first }
        Timber.tag("DebugEvent").d("""Log "${event.key}" values $params""")
    }
}

@Singleton
class AppTracker @Inject constructor(
    private val firebaseTracker: FirebaseTracker,
    private val debugTracker: DebugTracker,
) : Tracker {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val trackers: List<Tracker> by lazy {
        val list = mutableListOf<Tracker>()
        list.add(firebaseTracker)
        if (BuildConfig.DEBUG) {
            list.add(debugTracker)
        }

        list.toList()
    }

    override fun log(event: Event) {
        scope.launch {
            trackers.forEach { tracker ->
                tracker.log(event)
            }
        }
    }
}
