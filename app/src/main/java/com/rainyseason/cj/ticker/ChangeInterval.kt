package com.rainyseason.cj.ticker

// TODO remove 14 day support
@Suppress("ObjectPropertyName")
object ChangeInterval {
    const val _24H = "24h"
    const val _7D = "7d"
    const val _14D = "14d"
    const val _30D = "30d"
    const val _1Y = "1y"

    val ALL_PRICE_INTERVAL = listOf(_24H, _7D, _14D, _30D, _1Y)
}
