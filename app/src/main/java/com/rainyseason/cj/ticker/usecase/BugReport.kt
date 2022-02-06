package com.rainyseason.cj.ticker.usecase

import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
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
    val startTime = actualGraph.first()[0].toLong()
        .let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.of("+7")).toLocalDateTime() }
    val endTime = actualGraph.last()[0].toLong()
        .let { Instant.ofEpochMilli(it).atOffset(ZoneOffset.of("+7")).toLocalDateTime() }
    val delta = Duration.between(startTime, endTime)
    Timber.d("start time: $startTime")
    Timber.d("end time: $endTime")
    Timber.d("delta: $delta")
    Timber.d("candle percent: $percent")
}
