package com.rainyseason.cj.app

import javax.inject.Inject

class AppInitializer @Inject constructor(
    private val firebaseAnalyticInitializer: FirebaseAnalyticInitializer,
    private val oneSignalInitializer: OneSignalInitializer,
) {
    operator fun invoke() {
        listOf(
            firebaseAnalyticInitializer,
            oneSignalInitializer
        ).forEach {
            it.invoke()
        }
    }
}
