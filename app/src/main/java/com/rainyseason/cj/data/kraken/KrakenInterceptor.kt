package com.rainyseason.cj.data.kraken

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class KrakenInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
            .addHeader("origin", "https://trade.kraken.com")
            .build()
        return chain.proceed(newRequest)
    }
}
