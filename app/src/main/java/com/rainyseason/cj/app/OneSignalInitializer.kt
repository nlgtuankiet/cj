package com.rainyseason.cj.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.isUserLoginFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class OneSignalInitializer @Inject constructor(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val scope: CoroutineScope,
) : Function0<Unit> {

    /**
     * Enable
     * prod release -> true
     * prod debug   -> false
     * dev          -> true
     */
    override operator fun invoke() {
        scope.launch {
            val isDev = BuildConfig.FLAVOR == "dev"
            val isRelease = !BuildConfig.DEBUG
            val isEnable = isDev || isRelease

            if (!isEnable) {
                return@launch
            }

            if (BuildConfig.DEBUG) {
                OneSignal.setLogLevel(OneSignal.LOG_LEVEL.VERBOSE, OneSignal.LOG_LEVEL.NONE)
            }
            OneSignal.initWithContext(context)
            OneSignal.setAppId(BuildConfig.ONESIGNAL_APP_ID)
            firebaseAuth.isUserLoginFlow()
                .collect { isUserLogin ->
                    if (isUserLogin) {
                        val currentUser = firebaseAuth.currentUser
                        if (currentUser != null) {
                            OneSignal.setExternalUserId(currentUser.uid, "firebase_auth")
                        }
                    } else {
                        OneSignal.removeExternalUserId()
                    }
                }
        }
    }
}
