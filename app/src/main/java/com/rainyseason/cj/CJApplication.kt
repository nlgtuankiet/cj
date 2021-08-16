package com.rainyseason.cj

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksViewModelConfigFactory
import com.rainyseason.cj.common.NoopWorker
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CJApplication : Application(), HasAndroidInjector {

    @Inject
    @Volatile
    @JvmField
    var androidInjector: DispatchingAndroidInjector<Any>? = null

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var workManager: WorkManager

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
        injectIfNecessary()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Mavericks.initialize(
            context = this,
            viewModelConfigFactory = MavericksViewModelConfigFactory(
                debugMode = BuildConfig.DEBUG,
                contextOverride = Dispatchers.IO
            )
        )

        enqueueNoopWorker()
    }

    private fun enqueueNoopWorker() {
        val request = PeriodicWorkRequestBuilder<NoopWorker>(365, TimeUnit.DAYS)
            .build()
        workManager.enqueueUniquePeriodicWork("noop", ExistingPeriodicWorkPolicy.KEEP, request)
    }


    private fun injectIfNecessary() {
        if (androidInjector == null) {
            synchronized(this) {
                if (androidInjector == null) {
                    appComponent.inject(this)
                    if (androidInjector == null) {
                        throw IllegalStateException(
                            "The AndroidInjector returned from applicationInjector() did not inject the "
                                    + "DaggerApplication"
                        )
                    }
                }
            }
        }
    }

    override fun androidInjector(): AndroidInjector<Any?>? {
        injectIfNecessary()
        return androidInjector
    }
}