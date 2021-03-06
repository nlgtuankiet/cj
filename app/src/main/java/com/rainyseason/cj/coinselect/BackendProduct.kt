package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackendProduct(
    @Json(name = "id")
    val id: String,
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "display_name")
    val displayName: String,
    @Json(name = "backend")
    val backend: Backend,
    @Json(name = "icon_url")
    val iconUrl: String,
    @Json(name = "network")
    val network: String? = null,
    @Json(name = "dex")
    val dex: String? = null,
) {
    val uniqueId: String = "${backend.id}_${network}_${dex}_$id"
}
