package com.rainyseason.cj.widget.watch

import android.util.Size
import com.rainyseason.cj.R
import com.rainyseason.cj.common.model.WidgetLayout
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class WatchWidgetLayout(
    val id: String,
    val layout: Int,
    val previewScale: Double,
    val entryLimit: Int,
    override val providerName: String,
    val nameRes: Int,
    val ratio: Size,
) : WidgetLayout {
    Watch4x2(
        id = "watch_4x2",
        layout = R.layout.widget_watch_4x2_frame,
        previewScale = 0.75,
        entryLimit = 3,
        providerName = WatchWidget4x2Provider::class.java.name,
        nameRes = R.string.watch_widget_4x2,
        ratio = Size(4, 2)
    ),
    Watch4x4(
        id = "watch_4x4",
        layout = R.layout.widget_watch_4x4_frame,
        previewScale = 0.75,
        entryLimit = 6,
        providerName = WatchWidget4x4Provider::class.java.name,
        nameRes = R.string.watch_widget_4x4,
        ratio = Size(4, 4)
    ),
    ;

    companion object {
        fun fromProviderName(name: String): WatchWidgetLayout {
            return values().first { it.providerName == name }
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
