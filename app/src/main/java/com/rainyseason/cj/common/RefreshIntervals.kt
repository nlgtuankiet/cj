package com.rainyseason.cj.common

import android.content.Context
import com.rainyseason.cj.R
import java.util.concurrent.TimeUnit

object RefreshIntervals {
    val VALUES = listOf(
        15L to TimeUnit.SECONDS,
        30L to TimeUnit.SECONDS,
        1L to TimeUnit.MINUTES,
        5L to TimeUnit.MINUTES,
        15L to TimeUnit.MINUTES,
        30L to TimeUnit.MINUTES,
        1L to TimeUnit.HOURS,
        2L to TimeUnit.HOURS,
        3L to TimeUnit.HOURS,
        6L to TimeUnit.HOURS,
        12L to TimeUnit.HOURS,
        1L to TimeUnit.DAYS,
    )

    fun createString(
        context: Context,
        interval: Long,
        unit: TimeUnit,
    ): String {
        val res = when (unit) {
            TimeUnit.HOURS -> R.plurals.coin_ticker_preview_internal_hour_template
            TimeUnit.MINUTES -> R.plurals.coin_ticker_preview_internal_minute_template
            TimeUnit.DAYS -> R.plurals.coin_ticker_preview_internal_day_template
            TimeUnit.SECONDS -> R.plurals.coin_ticker_preview_internal_second_template
            else -> error("not support")
        }
        return context.resources.getQuantityString(res, interval.toInt(), interval)
    }
}
