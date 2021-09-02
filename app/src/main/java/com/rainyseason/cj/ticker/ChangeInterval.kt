package com.rainyseason.cj.ticker

object ChangeInterval {
    const val _24H = "24h"
    const val _7D = "7d"
    const val _14D = "14d"
    const val _30D = "30d"
    const val _60D = "60d"
    const val _1Y = "1y"

    val ALL_PRICE_INTERVAL = listOf(_24H, _7D, _14D, _30D, _60D, _1Y)
    val ALL_MARKET_CAP_INTERVAL = listOf(_24H)
}