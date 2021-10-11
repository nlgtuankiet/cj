package com.rainyseason.cj

import android.app.Application
import android.appwidget.AppWidgetManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airbnb.mvrx.Mavericks
import com.airbnb.mvrx.MavericksViewModelConfigFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rainyseason.cj.common.CoreComponent
import com.rainyseason.cj.common.HasCoreComponent
import com.rainyseason.cj.common.NoopWorker
import com.rainyseason.cj.common.getColorCompat
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
import androidx.annotation.ColorInt

import android.R

import com.rainyseason.cj.AppProvides_ContextFactory.context

import android.util.TypedValue




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
        checkFirebaseApp()
        injectIfNecessary()

        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        if (!BuildConfig.IS_PLAY_STORE) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(ExceptionLoggerTree())
        }

        val typedValue = TypedValue()
        theme.resolveAttribute(R.attr.textColorTertiary, typedValue, false)
        @ColorInt val colorInt = typedValue.data
        Timber.d("color is ${Integer.toHexString(getColorCompat(colorInt))}")

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
                            "The AndroidInjector returned from applicationInjector() " +
                                "did not inject the DaggerApplication"
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

    /**
     * In github we doesn't have google-services.json file, so initialize here
     */
    private fun checkFirebaseApp() {
        if (!BuildConfig.DEBUG) {
            return
        }
        val app = FirebaseApp.initializeApp(this)
        if (app == null && BuildConfig.IS_PLAY_STORE) {
            throw IllegalStateException("Invalid google play")
        }
        if (app == null) {
            FirebaseApp.initializeApp(
                this,
                FirebaseOptions.Builder()
                    .setApplicationId(packageName)
                    .setApiKey("= )))")
                    .build()
            )
        }
    }
}
