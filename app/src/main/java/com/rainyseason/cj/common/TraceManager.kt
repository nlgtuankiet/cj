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

    fun beginTrace(params: TraceParam) {
        val trace = firebasePerformance.newTrace(params.name)
        trace.start()
        traces[params.key] = trace
        if (debug) {
            Timber.d("Trace debug begin $params")
            debugTraces[params] = System.currentTimeMillis()
        }
    }

    fun cancelTrace(params: TraceParam) {
        val trace = traces.remove(params.key)
        if (debug) {
            if (trace != null) {
                Timber.d("Trace debug canceled $params")
            }
            debugTraces.remove(params)
        }
    }

    fun endTrace(params: TraceParam) {
        val trace = traces.remove(params.key)
        trace?.stop()
        if (debug) {
            val startTime = debugTraces.remove(params)
            if (startTime != null) {
                Timber.d("Trace debug end ${params.name} ${System.currentTimeMillis() - startTime}ms")
            }
        }
    }
}

data class CoinSelectTTI(
    override val key: String,
) : TraceParam(key = key, name = "coin_select_tti")

data class CoinTickerPreviewTTI(
    override val key: String,
) : TraceParam(key = "key", name = "coin_ticker_preview_tti")

open class TraceParam(open val key: String, val name: String)
