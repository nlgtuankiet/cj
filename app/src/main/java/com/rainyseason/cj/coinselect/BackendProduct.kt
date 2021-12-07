package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend

data class BackendProduct(
    val id: String,
    val symbol: String,
    val displayName: String,
    val backend: Backend
) {
    val uniqueId: String = "${backend.id}_$id"
}
