package com.rainyseason.cj.common.exception

import com.google.firebase.crashlytics.FirebaseCrashlytics

class FallbackPriceException(
    coinId: String, override val message: String = "fallback price for $coinId",
) : Exception()

fun FirebaseCrashlytics.logFallbackPrice(coinId: String) {
    recordException(FallbackPriceException(coinId))
}