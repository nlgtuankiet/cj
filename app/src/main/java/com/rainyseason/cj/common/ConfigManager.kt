package com.rainyseason.cj.common

import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
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
    private val tracker: Tracker
) {
    private val listeners = mutableListOf<ConfigChangeListener>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val firstFetchJob = scope.launch {
        val startTime = System.currentTimeMillis()
        val fetchResult = runCatching {
            firebaseRemoteConfig.fetch(1).await()
        }.onFailure {
            if (BuildConfig.DEBUG) {
                it.printStackTrace()
            }
        }

        val activateResult = runCatching {
            firebaseRemoteConfig.activate().await()
            notifyListeners()
        }.onFailure {
            if (BuildConfig.DEBUG) {
                it.printStackTrace()
            }
        }
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        tracker.logKeyParamsEvent(
            "remote_config_stats",
            mapOf(
                "fetch_success" to fetchResult.isSuccess,
                "activate_success" to activateResult.isSuccess,
                "time" to totalTime
            )
        )
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
