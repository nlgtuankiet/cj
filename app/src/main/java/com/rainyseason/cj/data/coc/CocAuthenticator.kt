package com.rainyseason.cj.data.coc

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CocAuthenticator @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.code != 401) {
            return null
        }

        if (responseCount(response) > 3) {
            Timber.d("too many attempt")
            return null
        }

        val currentUser = firebaseAuth.currentUser ?: return null
        val request = response.request
        val previousToken = request.headers["Authorization"]?.removePrefix("Bearer ")
        val newToken = synchronized(this) {
            Timber.d("getIdToken forceRefresh = false")
            var token = Tasks.await(currentUser.getIdToken(false)).token ?: return null
            if (token == previousToken) {
                // cached token is expired
                Timber.d("getIdToken forceRefresh = true")
                token = Tasks.await(currentUser.getIdToken(true)).token ?: return null
            }
            token
        }

        if (previousToken != newToken) {
            Timber.d("replace new token")
            return request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }

        return null
    }

    private fun responseCount(response: Response): Int {
        var currentResponse: Response? = response
        var result = 0
        while (currentResponse != null) {
            result++
            currentResponse = currentResponse.priorResponse
        }
        return result
    }
}
