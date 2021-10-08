package com.rainyseason.cj.data

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

class NetworkUrlLoggerInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        Timber.d("Network url: $url")
        return chain.proceed(chain.request())
    }
}
