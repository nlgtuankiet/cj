package com.rainyseason.cj.widget.watch

import com.rainyseason.cj.R
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class WatchWidgetLayout(
    val id: String,
    val layout: Int,
    val previewScale: Double,
) {
    Watch4x2("watch_4x2", R.layout.widget_watch_4x2_frame, 0.75),
    Watch4x4("watch_4x4", R.layout.widget_watch_4x2_frame, 0.75),
    ;

    companion object {
        // TODO rewrite
        fun fromDefaultLayout(id: Int): WatchWidgetLayout {
            return when (id) {
                R.layout.widget_watch_4x2_frame -> Watch4x2
                else -> error("?")
            }
        }
    }
}

object WatchWidgetLayoutJsonAdapter : JsonAdapter<WatchWidgetLayout>() {

    private val stringToEntry: Map<String, WatchWidgetLayout> = WatchWidgetLayout.values()
        .associateBy { it.id }

    override fun fromJson(reader: JsonReader): WatchWidgetLayout? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: WatchWidgetLayout?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}