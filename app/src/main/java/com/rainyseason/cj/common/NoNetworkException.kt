package com.rainyseason.cj.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import androidx.core.net.ConnectivityManagerCompat
import androidx.work.Logger
import androidx.work.impl.constraints.NetworkState
import androidx.work.impl.constraints.trackers.NetworkStateTracker
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NoNetworkException : Exception()

private fun Throwable.isNetworkRelated(): Boolean {
    return this is SocketTimeoutException
            || this is UnknownHostException
            || this is ConnectException
            || this is SocketException
}


fun Throwable.asNoNetworkException(context: Context): NoNetworkException? {
    val ex = this
    val hasValidNetworkConnection = context.hasValidNetworkConnection()
    return if (ex.isNetworkRelated() && !hasValidNetworkConnection) {
        NoNetworkException()
    } else {
        null
    }
}


/**
 * Copy from Work Manager
 */
fun Context.hasValidNetworkConnection(): Boolean {
    val manager = getSystemService<ConnectivityManager>()!!
    return if (Build.VERSION.SDK_INT >= 26) {
        manager.isConnected() && manager.isActiveNetworkValidated()
    } else {
        manager.isConnected()
    }
}

@Suppress("deprecation")
private fun ConnectivityManager.isConnected(): Boolean {
    val info: NetworkInfo? = activeNetworkInfo
    return info != null && info.isConnected
}


private fun ConnectivityManager.isActiveNetworkValidated(): Boolean {
    return if (Build.VERSION.SDK_INT < 23) {
        false // NET_CAPABILITY_VALIDATED not available until API 23. Used on API 26+.
    } else try {
        val capabilities = getNetworkCapabilities(activeNetwork) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (exception: SecurityException) {
        // b/163342798
        false
    }
}