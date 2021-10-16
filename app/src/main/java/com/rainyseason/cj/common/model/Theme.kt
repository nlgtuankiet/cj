package com.rainyseason.cj.common.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class Theme(val id: String) {
    Auto("auto"),
    Light("light"),
    Dark("dark"),
}

object ThemeJsonAdapter : JsonAdapter<Theme>() {

    private val stringToEntry: Map<String, Theme> = Theme.values()
        .associateBy { it.id }

    override fun fromJson(reader: JsonReader): Theme? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: Theme?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}
