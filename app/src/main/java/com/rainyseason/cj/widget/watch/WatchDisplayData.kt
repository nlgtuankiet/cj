package com.rainyseason.cj.widget.watch

import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.model.TimeInterval
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WatchDisplayData(
    @Json(name = "entries")
    val entries: List<WatchDisplayEntry>
)

@JsonClass(generateAdapter = true)
data class WatchDisplayEntry(
    @Json(name = "coin_id")
    val coinId: String,

    @Json(name = "backend")
    val backend: Backend = Backend.CoinGecko,

    @Json(name = "network")
    val network: String? = null,

    @Json(name = "dex")
    val dex: String? = null,

    @Json(name = "content")
    val content: WatchDisplayEntryContent?
) {
    val coin: Coin = Coin(coinId, backend, network, dex)
}

data class WatchDisplayEntryLoadParam(
    val coin: Coin,
    val currency: String,
    val changeInterval: TimeInterval,
)

@JsonClass(generateAdapter = true)
data class WatchDisplayEntryContent(
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "graph")
    val graph: List<List<Double>>?,
    @Json(name = "price")
    val price: Double,
    @Json(name = "change_percent")
    val changePercent: Double?
)
