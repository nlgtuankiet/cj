package com.rainyseason.cj.data.coc

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.internal.IdTokenListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.internal.InternalTokenResult
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Attempt to attach bearer token if any
 */
@Singleton
class CoinOmegaCoinInterceptor @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firebaseCrashlytics: FirebaseCrashlytics,
) : Interceptor, IdTokenListener {

    private var cachedToken: String? = null

    init {
        Timber.d("Register listener")
        firebaseAuth.addIdTokenListener(this)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (firebaseAuth.currentUser == null) {
            // user not logged in nothing to do here
            return chain.proceed(chain.request())
        }

        var currentToken = cachedToken
        Timber.d("Intercept ${chain.request().url}, has token: ${currentToken != null}")
        if (currentToken.isNullOrBlank()) {
            synchronized(this) {
                currentToken = cachedToken
                if (currentToken.isNullOrBlank()) {
                    try {
                        currentToken = firebaseAuth.currentUser?.let { user ->
                            Tasks.await(user.getIdToken(false)).token
                        }.also {
                            cachedToken = it
                        }
                    } catch (ex: Exception) {
                        firebaseCrashlytics.recordException(CocInterceptorGetTokenFailed(ex))
                    }
                }
            }
        }

        if (!currentToken.isNullOrBlank()) {
            val newRequest = chain.request().newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
            return chain.proceed(newRequest)
        }

        return chain.proceed(chain.request())
    }

    override fun onIdTokenChanged(result: InternalTokenResult) {
        cachedToken = result.token
        Timber.d("Token changed to $cachedToken")
    }
}

class CocInterceptorGetTokenFailed(cause: Throwable) : Exception(cause)
