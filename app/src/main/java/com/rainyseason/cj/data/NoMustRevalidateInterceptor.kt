package com.rainyseason.cj.data

import com.rainyseason.cj.data.coingecko.CoinGeckoService
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject

class NoMustRevalidateInterceptor @Inject constructor(

) : Interceptor {
    private val removeUrls = listOf(
        "${CoinGeckoService.BASE_URL}coins/markets",
        "${CoinGeckoService.BASE_URL}coins/list"
    )


    /**
     * Remove must-revalidate header
     * This will help bypass validate cache response
     * ex:
     *    cache-control: max-age=30, public, must-revalidate, s-maxage=300
     * -> cache-control: max-age=30, public, , s-maxage=300
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val urlString = request.url.toString()
        val shouldRemove = removeUrls.any { urlString.startsWith(it) }
        if (!shouldRemove) {
            return response
        }

        val cacheControl = response.headers.firstOrNull {
            it.first.equals("Cache-Control", ignoreCase = true)
        } ?: return response
        Timber.d("old cache control: ${response.cacheControl}")
        val newResponse = response.newBuilder()
            .header(
                name = "Cache-Control",
                value = cacheControl.second.replace(
                    oldValue = "must-revalidate",
                    newValue = "",
                    ignoreCase = true
                ).replace(", , ", ", ")
            )
            .build()
        Timber.d("new cache control: ${newResponse.cacheControl}")
        return newResponse
    }
}