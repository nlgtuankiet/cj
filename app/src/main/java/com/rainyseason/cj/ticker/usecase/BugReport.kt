package com.rainyseason.cj.ticker.usecase

import com.google.firebase.crashlytics.BuildConfig
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import timber.log.Timber
import kotlin.math.abs

@Suppress("NOTHING_TO_INLINE")
inline fun reportIntervalPercent(
    loadParams: CoinTickerDisplayData.LoadParam,
    actualGraph: List<List<Double>>,
) {
    if (!BuildConfig.DEBUG) {
        return
    }
    val expectedIntervalMilis = loadParams.changeInterval.toMilis()
    val outputIntervalMilis = actualGraph.last()[0] - actualGraph.first()[0]
    val percent = abs(1 - outputIntervalMilis * 1.0 / expectedIntervalMilis)
    Timber.d("candle percent: $percent")
}
