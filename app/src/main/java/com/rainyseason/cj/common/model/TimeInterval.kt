package com.rainyseason.cj.common.model

import com.rainyseason.cj.R
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.util.concurrent.TimeUnit

enum class TimeInterval(
    val id: String,
    val titleRes: Int,
) {
    I_1H(
        id = "1h",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_1h,
    ),
    I_24H(
        id = "24h",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_24h,
    ),
    I_7D(
        id = "7d",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_7d
    ),
    I_14D(
        id = "14d",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_14d
    ),
    I_30D(
        id = "30d",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_30d
    ),
    I_90D(
        id = "90d",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_90d
    ),
    I_180D(
        id = "180d",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_180d
    ),
    I_1Y(
        id = "1y",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_1y
    ),
    I_ALL(
        id = "all",
        titleRes = R.string.coin_ticker_preview_setting_bottom_change_percent_interval_all
    ),
    ;

    fun toMilis(): Long {
        return when (this) {
            I_1H -> TimeUnit.HOURS.toMillis(1)
            I_24H -> TimeUnit.HOURS.toMillis(24)
            I_7D -> TimeUnit.DAYS.toMillis(7)
            I_14D -> TimeUnit.DAYS.toMillis(14)
            I_30D -> TimeUnit.DAYS.toMillis(30)
            I_90D -> TimeUnit.DAYS.toMillis(90)
            I_180D -> TimeUnit.DAYS.toMillis(180)
            I_1Y -> TimeUnit.DAYS.toMillis(365)
            else -> error("not support")
        }
    }

    fun toSeconds(): Long {
        return toMilis() / 1000
    }
}

private val intervalToDayString = mapOf(
    TimeInterval.I_1H to null,
    TimeInterval.I_24H to "1",
    TimeInterval.I_7D to "7",
    TimeInterval.I_14D to "14",
    TimeInterval.I_30D to "30",
    TimeInterval.I_90D to "90",
    TimeInterval.I_180D to "180",
    TimeInterval.I_1Y to "365",
    TimeInterval.I_ALL to "max",
)

/**
 * For coingecko api
 */
fun TimeInterval.asDayString(): String? {
    return intervalToDayString[this]
}

object TimeIntervalJsonAdapter : JsonAdapter<TimeInterval>() {

    private val stringToEntry: Map<String, TimeInterval> = TimeInterval.values()
        .associateBy { it.id }

    /**
     *     const val _24H = "24h"
     *     const val _7D = "7d"
     *     const val _14D = "14d"
     *     const val _30D = "30d"
     *     const val _1Y = "1y"
     */
    override fun fromJson(reader: JsonReader): TimeInterval? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: TimeInterval?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}
