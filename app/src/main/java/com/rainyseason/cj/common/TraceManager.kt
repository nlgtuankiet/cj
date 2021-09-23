package com.rainyseason.cj.common

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.rainyseason.cj.BuildConfig
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TraceManager @Inject constructor(
    private val firebasePerformance: FirebasePerformance,
) {
    private val traces = Collections.synchronizedMap(mutableMapOf<String, Trace>())
    private val debugTraces = mutableMapOf<TraceParam, Long>()
    private val debug = BuildConfig.DEBUG

    fun beginTrace(key: String, name: String) {
        val trace = firebasePerformance.newTrace(name)
        trace.start()
        traces[key] = trace
    }

    fun beginTrace(params: TraceParam) {
        beginTrace(key = params.key, name = params.name)
        if (debug) {
            debugTraces[params] = System.currentTimeMillis()
        }
    }

    fun endTrace(key: String) {
        val trace = traces.remove(key)
        trace?.stop()
    }

    fun endTrace(params: TraceParam) {
        endTrace(key = params.key)
        if (debug) {
            val startTime = debugTraces.remove(params)
            if (startTime != null) {
                Timber.d("Trace debug ${params.name} ${System.currentTimeMillis() - startTime}ms")
            }
        }
    }
}

data class CoinTickerListTTI(
    val widgetId: Int,
): TraceParam(key = "coin_ticker_list_tti_$widgetId", name = "coin_ticker_list_tti")
open class TraceParam(val key: String, val name: String)