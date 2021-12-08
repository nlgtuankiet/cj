package com.rainyseason.cj.data.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class SyncInterceptor(
    val source: Interceptor
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = synchronized(lock) {
            source.intercept(chain)
        }
        return response
    }

    companion object {
        val lock = Any()
    }
}

fun Interceptor.synchronized(): Interceptor {
    return SyncInterceptor(this)
}
