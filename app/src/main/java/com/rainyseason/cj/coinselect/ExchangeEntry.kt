package com.rainyseason.cj.coinselect

import com.rainyseason.cj.common.model.Backend

data class ExchangeEntry(
    val symbol: String,
    val displayName: String,
    val backend: Backend
) {
    val uniqueId: String = "${backend.id}_$symbol"
}
