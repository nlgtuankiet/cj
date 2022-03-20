package com.rainyseason.cj.data.coc

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.internal.IdTokenListener
import com.google.firebase.internal.InternalTokenResult
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All request to CoinOmegaCoin backend will attach firebase auth id token
 * TODO maybe we want to use authenticator?
 */
@Singleton
class CoinOmegaCoinInterceptor @Inject constructor(
    firebaseAuth: FirebaseAuth,
) : Interceptor, IdTokenListener {

    private var token: String? = null

    init {
        Timber.d("init")
        val startTime = System.currentTimeMillis()
        firebaseAuth.addIdTokenListener(this)
        firebaseAuth.currentUser?.getIdToken(false)?.addOnCompleteListener {
            val endTime = System.currentTimeMillis()
            if (it.isSuccessful) {
                token = it.result.token
                Timber.d("Token init to $token")
            }
            Timber.d("init token take: ${endTime - startTime}ms")
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val currentToken = token
        Timber.d("Intercept ${chain.request().url}, has token: ${currentToken != null}")
        if (currentToken != null) {
            val newRequest = chain.request().newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(chain.request())
    }

    override fun onIdTokenChanged(result: InternalTokenResult) {
        token = result.token
        Timber.d("Token changed to $token")
    }
}
