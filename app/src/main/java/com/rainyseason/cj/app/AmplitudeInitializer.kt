package com.rainyseason.cj.app

import android.content.Context
import com.amplitude.api.Amplitude
import com.amplitude.api.AmplitudeClient
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.checkNotMainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AmplitudeInitializer @Inject constructor(
    private val amplitudeClientProvider: Provider<AmplitudeClient>,
    private val coroutineScope: CoroutineScope,
    private val firebaseAuth: FirebaseAuth,
    private val context: Context,
    private val clientProvider: Provider<OkHttpClient>,
) : Function0<Unit> {
    override operator fun invoke() {
        coroutineScope.launch {
            val client = amplitudeClientProvider.get()
            firebaseAuth.addAuthStateListener {
                coroutineScope.launch {
                    client.userId = firebaseAuth.currentUser?.uid
                }
            }
        }
    }

    fun initAndGetInstance(): AmplitudeClient {
        checkNotMainThread()
        val instance = Amplitude.getInstance()
        instance.initialize(
            context,
            BuildConfig.AMPLITUDE_KEY,
            firebaseAuth.currentUser?.uid,
            null,
            BuildConfig.DEBUG
        ) { clientProvider.get().newCall(it) }
        return instance
    }
}
