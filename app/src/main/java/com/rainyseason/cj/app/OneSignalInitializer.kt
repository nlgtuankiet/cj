package com.rainyseason.cj.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.isUserLoginFlow
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class OneSignalInitializer @Inject constructor(
    private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val scope: CoroutineScope,
) {

    operator fun invoke() {
        scope.launch {
            if (DebugFlag.DISABLE_ONESIGNAL.isEnable) {
                Timber.d("Disable one signal")
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
