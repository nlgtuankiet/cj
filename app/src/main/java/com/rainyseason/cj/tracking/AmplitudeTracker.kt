package com.rainyseason.cj.tracking

import com.amplitude.api.AmplitudeClient
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AmplitudeTracker @Inject constructor(
    private val amplitudeClientProvider: Provider<AmplitudeClient>,
    private val firebaseCrashlytics: Provider<FirebaseCrashlytics>
) : SyncTracker {
    private val client: AmplitudeClient
        get() = amplitudeClientProvider.get()

    override fun log(event: Event) {
        when (event) {
            is KeyParamsEvent -> logKeyParamsEvent(event)
        }
    }

    private fun logKeyParamsEvent(event: KeyParamsEvent) {
        val json = JSONObject()
        event.params.forEach { (key, value) ->
            when (value) {
                is Int -> json.put(key, value)
                is Long -> json.put(key, value)
                is String -> json.put(key, value)
                is Boolean -> json.put(key, value)
                is Double -> json.put(key, value)
                is Float -> json.put(key, value)
                else -> recordUnknownParamType(firebaseCrashlytics, event, key)
            }
        }
        client.logEvent(event.key, json)
    }
}
