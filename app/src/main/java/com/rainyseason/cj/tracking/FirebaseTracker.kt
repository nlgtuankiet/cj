package com.rainyseason.cj.tracking

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.rainyseason.cj.BuildConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class FirebaseTracker @Inject constructor(
    private val firebaseAnalytics: Provider<FirebaseAnalytics>,
) : SyncTracker {

    private val whitelistEvent = arrayOf(
        EventName.WIDGET_REFRESH,
        EventName.WIDGET_REFRESH_FAIL
    )

    override fun log(event: Event) {
        if (event !is KeyParamsEvent) {
            return
        }
        if (event.key !in whitelistEvent) {
            return
        }

        if (BuildConfig.DEBUG) {
            Timber.d("Log ${event.key}")
        }

        firebaseAnalytics.get().logEvent(event.key, Bundle.EMPTY)
    }
}
