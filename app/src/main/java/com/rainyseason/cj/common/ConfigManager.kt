package com.rainyseason.cj.common

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.rainyseason.cj.tracking.Tracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigManager @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
    private val firebasePerformance: FirebasePerformance,
    private val tracker: Tracker
) {
    private val listeners = mutableListOf<ConfigChangeListener>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val firstFetchJob = scope.launch {
        val configTrace = firebasePerformance.newTrace("remote_config")
        configTrace.start()

        val fetchStartTime = System.currentTimeMillis()
        val fetchResult = runCatching {
            firebaseRemoteConfig.fetch(1).await()
        }.onFailure {
            if (BuildConfig.DEBUG) {
                it.printStackTrace()
            }
        }
        val fetchEndTime = System.currentTimeMillis()
        val activateResult = runCatching {
            firebaseRemoteConfig.activate().await()
            notifyListeners()
        }.onFailure {
            if (BuildConfig.DEBUG) {
                it.printStackTrace()
            }
        }
        val activeEndTime = System.currentTimeMillis()
        configTrace.putMetric("fetch", fetchEndTime - fetchStartTime)
        configTrace.putMetric("active", activeEndTime - fetchEndTime)
        configTrace.putMetric("fetch_active", activeEndTime - fetchStartTime)
        configTrace.putAttribute("fetch_success", fetchResult.isSuccess.toString())
        configTrace.putAttribute("activate_success", activateResult.isSuccess.toString())
        configTrace.stop()
    }

    suspend fun awaitFirstFetch() {
        firstFetchJob.join()
    }

    fun registerListener(listener: ConfigChangeListener) {
        scope.launch {
            listeners.add(listener)
            listener.onConfigChange()
        }
    }

    private fun notifyListeners() {
        scope.launch {
            listeners.forEach { it.onConfigChange() }
        }
    }
}

interface ConfigChangeListener {
    fun onConfigChange()
}
