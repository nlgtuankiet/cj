package com.rainyseason.cj.app

import javax.inject.Inject

class AppInitializer @Inject constructor(
    private val firebaseAnalyticInitializer: FirebaseAnalyticInitializer,
    private val oneSignalInitializer: OneSignalInitializer,
    private val amplitudeInitializer: AmplitudeInitializer,
) {
    operator fun invoke() {
        listOf(
            firebaseAnalyticInitializer,
            oneSignalInitializer,
            amplitudeInitializer,
        ).forEach {
            it.invoke()
        }
    }
}
