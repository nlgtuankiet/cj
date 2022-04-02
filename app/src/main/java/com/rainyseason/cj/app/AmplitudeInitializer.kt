package com.rainyseason.cj.app

import com.amplitude.api.AmplitudeClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class AmplitudeInitializer @Inject constructor(
    private val amplitudeClient: Provider<AmplitudeClient>,
    private val coroutineScope: CoroutineScope,
    private val firebaseAuth: FirebaseAuth,
) {
    operator fun invoke() {
        coroutineScope.launch {
            val client = amplitudeClient.get()
            firebaseAuth.addAuthStateListener {
                coroutineScope.launch {
                    client.userId = firebaseAuth.currentUser?.uid
                }
            }
        }
    }
}
