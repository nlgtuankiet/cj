package com.rainyseason.cj.common.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class Exchange(
    val id: String,
    val displayName: String,
    val iconUrl: String
) {
    Binance(
        id = "binance",
        displayName = "Binance",
        iconUrl = "https://s2.coinmarketcap.com/static/img/coins/128x128/1839.png"
    )
    ;

    companion object {
        fun from(id: String): Exchange {
            return values().first { it.id == id }
        }
    }
}

object ExchangeJsonAdapter : JsonAdapter<Exchange>() {

    private val stringToEntry: Map<String, Exchange> = Exchange.values()
        .associateBy { it.id }

    override fun fromJson(reader: JsonReader): Exchange? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: Exchange?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}
