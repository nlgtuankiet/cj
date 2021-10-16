package com.rainyseason.cj.widget.watch

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class WatchClickAction(val id: String) {
    OpenWatchlist("open_watchlist"),
    Refresh("refresh"),
    ;

    companion object {
        const val NAME = "com.rainyseason.cj.widget.watchlist.click"
    }
}

object WatchClickActionJsonAdapter : JsonAdapter<WatchClickAction>() {

    private val stringToEntry: Map<String, WatchClickAction> = WatchClickAction.values()
        .associateBy { it.id }

    override fun fromJson(reader: JsonReader): WatchClickAction? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: WatchClickAction?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}
