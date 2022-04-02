package com.rainyseason.cj.app

import javax.inject.Inject

class AppInitializer @Inject constructor(
    private val firebaseAnalyticInitializer: FirebaseAnalyticInitializer,
) {
    operator fun invoke() {
        listOf(
            firebaseAnalyticInitializer
        ).forEach {
            it.invoke()
        }
    }
}
