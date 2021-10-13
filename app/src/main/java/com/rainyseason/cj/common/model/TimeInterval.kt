package com.rainyseason.cj.common.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class TimeInterval(val id: String) {
    I_1H("1h"),
    I_24H("24h"),
    I_7D("7d"),
    I_30D("30d"),
    I_90D("90d"),
    I_1Y("1y"),
    I_ALL("all"),
}

private val intervalToDayString = mapOf(
    TimeInterval.I_1Y to null,
    TimeInterval.I_24H to "1",
    TimeInterval.I_7D to "7",
    TimeInterval.I_30D to "30",
    TimeInterval.I_90D to "90",
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