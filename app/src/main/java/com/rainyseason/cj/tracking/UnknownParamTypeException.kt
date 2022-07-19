package com.rainyseason.cj.tracking

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.BuildConfig
import javax.inject.Provider

class UnknownParamTypeException(
    eventName: String,
    paramKey: String,
    paramValue: String,
) : IllegalArgumentException(
    "Unknown param type for event $eventName, param key: $paramKey, param value: $paramValue"
)

@Suppress("NOTHING_TO_INLINE")
inline fun recordUnknownParamType(
    firebaseCrashlytics: Provider<FirebaseCrashlytics>,
    event: KeyParamsEvent,
    paramKey: String,
) {
    val value = event.params[paramKey]
    if (value != null) {
        val ex = UnknownParamTypeException(
            eventName = event.key,
            paramKey = paramKey,
            paramValue = value.toString(),
        )
        if (BuildConfig.DEBUG) {
            throw ex
        } else {
            firebaseCrashlytics.get().recordException(ex)
        }
    }
}