package com.rainyseason.cj.common.model

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class Coin(
    @Json(name = "id")
    val id: String,

    @Json(name = "backend")
    val backend: Backend = Backend.CoinGecko,

    @Json(name = "network")
    val network: String? = null,

    @Json(name = "dex")
    val dex: String? = null,
) : Parcelable {
    fun getTrackingParams(): Map<String, Any?> {
        return mapOf(
            "coin_id" to id,
            "backend" to backend.id,
            "network" to network,
            "dex" to dex,
        )
    }
}
