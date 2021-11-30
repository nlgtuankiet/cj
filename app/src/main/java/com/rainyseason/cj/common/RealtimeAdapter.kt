package com.rainyseason.cj.common

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

fun <T> Context.realtimeFlowOf(
    block: suspend () -> T
): Flow<T> {
    return flow {
        while (currentCoroutineContext().isActive) {
            awaitAppForeground()
            awaitValidatedNetwork()
            try {
                emit(block())
            } catch (ex: Exception) {
                Timber.e(ex)
            }
            awaitRealtimeInterval()
        }
    }
}

private suspend fun Context.awaitRealtimeInterval() {
    val userSettingRepository = coreComponent.userSettingRepository
    val ms = userSettingRepository.getUserSetting().realtimeIntervalMs
    delay(ms)
}

private suspend fun Context.awaitValidatedNetwork() {
    if (hasValidNetworkConnection()) {
        return
    }
    if (Build.VERSION.SDK_INT >= 24) {
        awaitApi24ValidatedNetwork()
    } else {
        awaitLegacyValidatedNetwork()
    }
}

@RequiresApi(24)
private suspend fun Context.awaitApi24ValidatedNetwork() {
    val manager = getSystemService<ConnectivityManager>()!!
    suspendCancellableCoroutine<Unit> { cont ->
        val callback = object : NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (hasValidNetworkConnection()) {
                    manager.unregisterNetworkCallback(this)
                    cont.resume(Unit)
                }
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        cont.invokeOnCancellation {
            manager.unregisterNetworkCallback(callback)
        }
    }
}

private suspend fun Context.awaitLegacyValidatedNetwork() {

    suspendCancellableCoroutine<Unit> { cont ->
        val receiver = NetworkStateBroadcastReceiver(
            block = {
                if (hasValidNetworkConnection()) {
                    unregisterReceiver(it)
                    cont.resume(Unit)
                }
            }
        )
        cont.invokeOnCancellation {
            unregisterReceiver(receiver)
        }
        registerReceiver(receiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }
}

private class NetworkStateBroadcastReceiver(
    private val block: (NetworkStateBroadcastReceiver) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null || intent.action == null) {
            return
        }
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            block(this)
        }
    }
}

private suspend fun awaitAppForeground() {
    suspendCancellableCoroutine<Unit> { cont ->
        val processOwner = ProcessLifecycleOwner.get().lifecycle
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                super.onResume(owner)
                processOwner.removeObserver(this)
                cont.resume(Unit)
            }
        }
        runBlocking(Dispatchers.Main) {
            cont.invokeOnCancellation {
                processOwner.removeObserver(observer)
            }
            processOwner.addObserver(observer)
        }
    }
}

interface DefaultLifecycleObserver : LifecycleEventObserver {
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate(source)
            Lifecycle.Event.ON_START -> onStart(source)
            Lifecycle.Event.ON_RESUME -> onResume(source)
            Lifecycle.Event.ON_PAUSE -> onPause(source)
            Lifecycle.Event.ON_STOP -> onStop(source)
            Lifecycle.Event.ON_DESTROY -> onDestroy(source)
            Lifecycle.Event.ON_ANY ->
                throw IllegalArgumentException("ON_ANY must not been send by anybody")
        }
    }

    fun onCreate(owner: LifecycleOwner) {
    }

    fun onStart(owner: LifecycleOwner) {
    }

    fun onResume(owner: LifecycleOwner) {
    }

    fun onPause(owner: LifecycleOwner) {
    }

    fun onStop(owner: LifecycleOwner) {
    }

    fun onDestroy(owner: LifecycleOwner) {
    }
}
