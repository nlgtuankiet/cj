package com.rainyseason.cj.app

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.checkNotMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticInitializer @Inject constructor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val firebaseAnalyticsProvider: Provider<FirebaseAnalytics>,
    private val firebaseAuth: FirebaseAuth,
) : Function0<Unit> {
    private val firebaseAnalytics: FirebaseAnalytics
        get() = firebaseAnalyticsProvider.get()

    override operator fun invoke() {
        scope.launch {
            firebaseAnalyticsProvider.get() // init on background
            firebaseAuth.addAuthStateListener {
                firebaseAnalytics.setUserId(firebaseAuth.currentUser?.uid)
            }
        }
    }

    fun initAndGetInstance(): FirebaseAnalytics {
        checkNotMainThread()
        return FirebaseAnalytics.getInstance(context).apply {
            setAnalyticsCollectionEnabled(BuildConfig.IS_PLAY_STORE)
        }
    }
}
