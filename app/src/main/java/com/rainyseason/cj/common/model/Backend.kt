package com.rainyseason.cj.common.model

import com.rainyseason.cj.common.CurrencyInfo
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

private val defaultTimeRanges = setOf(
    TimeInterval.I_24H,
    TimeInterval.I_7D,
    TimeInterval.I_30D,
)

enum class Backend(
    val id: String,
    val displayName: String,
    val iconUrl: String,
    val isExchange: Boolean,
    val isDefault: Boolean = false,
    val supportedTimeRange: Set<TimeInterval> = defaultTimeRanges,
    val canSearchProduct: Boolean = false,
    val hasCoinUrl: Boolean = false,
    val supportedCurrency: List<CurrencyInfo> = listOf(CurrencyInfo.NONE),
) {
    CoinGecko(
        id = "coin_gecko",
        displayName = "CoinGecko",
        iconUrl = "https://www.coingecko.com/favicon-96x96.png",
        isExchange = false,
        isDefault = true,
        supportedTimeRange = setOf(
            TimeInterval.I_24H,
            TimeInterval.I_7D,
            TimeInterval.I_14D,
            TimeInterval.I_30D,
            TimeInterval.I_90D,
            TimeInterval.I_180D,
            TimeInterval.I_1Y,
        ),
        supportedCurrency = SUPPORTED_CURRENCY.values.sortedBy { it.code },
        hasCoinUrl = true,
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
        isExchange = false,
        supportedCurrency = SUPPORTED_CURRENCY
            .filter { it.value.cmcId != null }
            .map { it.value }
            .sortedBy { it.code }
        ,
        hasCoinUrl = true,
    ),
    Coinbase(
        id = "coinbase",
        displayName = "Coinbase",
        iconUrl = "https://s2.coinmarketcap.com/static/img/exchanges/128x128/89.png",
        isExchange = true
    ),
    Ftx(
        id = "ftx",
        displayName = "FTX",
        iconUrl = "https://s2.coinmarketcap.com/static/img/exchanges/128x128/524.png",
        isExchange = true
    ),
    Kraken(
        id = "kraken",
        displayName = "Kraken",
        iconUrl = "https://s2.coinmarketcap.com/static/img/exchanges/128x128/24.png",
        isExchange = true
    ),
    Luno(
        id = "luno",
        displayName = "Luno",
        iconUrl = "https://s2.coinmarketcap.com/static/img/exchanges/128x128/171.png",
        isExchange = true
    ),
    DexScreener(
        id = "dex_screener",
        displayName = "DEX Screener",
        iconUrl = "https://dexscreener.com/favicon.png",
        isExchange = false,
        canSearchProduct = true,
        supportedCurrency = listOf(
            CurrencyInfo.NONE,
            CurrencyInfo.USD,
        )
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
