package com.rainyseason.cj

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksViewModelConfigFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rainyseason.cj.common.CoreComponent
import com.rainyseason.cj.common.HasCoreComponent
import com.rainyseason.cj.common.NoopWorker
import com.rainyseason.cj.featureflag.DebugFlagProvider
import com.rainyseason.cj.featureflag.MainFlagValueProvider
import com.rainyseason.cj.featureflag.NoopFlagValueProvider
import com.rainyseason.cj.util.ExceptionLoggerTree
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Provider

class CJApplication : Application(), HasAndroidInjector, HasCoreComponent {

    @Inject
    @Volatile
    @JvmField
    var androidInjector: DispatchingAndroidInjector<Any>? = null

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var debugFlagProvider: Provider<DebugFlagProvider>

    @Inject
    lateinit var firebasePerformance: FirebasePerformance

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
        injectIfNecessary()

        if (!BuildConfig.IS_PLAY_STORE) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(ExceptionLoggerTree())
        }

        if (!BuildConfig.IS_PLAY_STORE) {
            MainFlagValueProvider.setDelegate(debugFlagProvider.get())
        } else {
            MainFlagValueProvider.setDelegate(NoopFlagValueProvider)
        }

        firebasePerformance.isPerformanceCollectionEnabled = BuildConfig.IS_PLAY_STORE

        firebaseAnalytics.setAnalyticsCollectionEnabled(BuildConfig.IS_PLAY_STORE)

        Mavericks.initialize(
            context = this,
            viewModelConfigFactory = MavericksViewModelConfigFactory(
                debugMode = BuildConfig.DEBUG,
                contextOverride = Dispatchers.IO
            )
        )

        enqueueNoopWorker()

        AndroidThreeTen.init(this)

        if (BuildConfig.DEBUG) {
            debugFlagProvider.get().awaitFirstValue()
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(false)
                deleteUnsentReports()
            }
        }
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
                            "The AndroidInjector returned from applicationInjector() did not inject the " +
                                "DaggerApplication"
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

    override val coreComponent: CoreComponent
        get() = appComponent
}
