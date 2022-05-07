package com.rainyseason.cj.app

import javax.inject.Inject

class AppInitializer @Inject constructor(
    private val firebaseAnalyticInitializer: FirebaseAnalyticInitializer,
    private val oneSignalInitializer: OneSignalInitializer,
    private val amplitudeInitializer: AmplitudeInitializer,
    private val configManagerInitializer: ConfigManagerInitializer,
) {
    operator fun invoke() {
        listOf(
            firebaseAnalyticInitializer,
            oneSignalInitializer,
            amplitudeInitializer,
            configManagerInitializer
        ).forEach {
            it.invoke()
        }
    }
}
