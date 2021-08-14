package com.rainyseason.cj

import android.app.Application
import timber.log.Timber

class CJApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}