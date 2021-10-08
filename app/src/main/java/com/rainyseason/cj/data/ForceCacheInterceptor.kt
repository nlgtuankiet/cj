package com.rainyseason.cj.data

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class ForceCacheInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url
        val newUrl = url.newBuilder().removeAllQueryParameters(Signal.FORCE_CACHE).build()
        if (url.queryParameter(Signal.FORCE_CACHE) != "true") {
            return chain.proceed(chain.request().newBuilder().url(newUrl).build())
        }
        val request = chain.request()
        val newRequest = request.newBuilder()
            .url(newUrl)
            .cacheControl(CacheControl.FORCE_CACHE)
            .build()
        return chain.proceed(newRequest)
    }
}
