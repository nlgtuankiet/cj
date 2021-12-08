package com.rainyseason.cj.common.model

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class Backend(
    val id: String,
    val displayName: String,
    val iconUrl: String,
    val isExchange: Boolean,
    val isDefault: Boolean = false,
) {
    CoinGecko(
        id = "coin_gecko",
        displayName = "CoinGecko",
        iconUrl = "https://www.coingecko.com/favicon-96x96.png",
        isExchange = false,
        isDefault = true
    ),
    Binance(
        id = "binance",
        displayName = "Binance",
        iconUrl = "https://s2.coinmarketcap.com/static/img/coins/128x128/1839.png",
        isExchange = true
    ),
    CoinMarketCap(
        id = "coinmarketcap",
        displayName = "CoinMarketCap",
        iconUrl = "https://coinmarketcap.com/apple-touch-icon.png",
        isExchange = false
    ),
    ;

    companion object {
        fun from(id: String?): Backend {
            if (id == null) {
                return CoinGecko
            }
            return values().first { it.id == id }
        }
    }
}

object BackendJsonAdapter : JsonAdapter<Backend>() {

    private val stringToEntry: Map<String, Backend> = Backend.values()
        .associateBy { it.id }

    override fun fromJson(reader: JsonReader): Backend? {
        val stringValue = reader.nextString() ?: return null
        return stringToEntry[stringValue]
    }

    override fun toJson(writer: JsonWriter, value: Backend?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.id)
        }
    }
}
