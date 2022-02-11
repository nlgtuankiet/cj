package com.rainyseason.cj.common.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Watchlist(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "coins")
    val coins: List<Coin>
) {
    companion object {
        const val DEFAULT_ID = "default"
        val DEFAULT_EMPTY = Watchlist(id = DEFAULT_ID, coins = emptyList())
    }
}

@JsonClass(generateAdapter = true)
data class WatchlistCollection(
    @Json(name = "list")
    val list: List<Watchlist>,
) {
    companion object {
        val EMPTY = WatchlistCollection(emptyList())
    }
}
