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
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.jakewharton.threetenabp.AndroidThreeTen
import com.rainyseason.cj.app.AmplitudeInitializer
import com.rainyseason.cj.app.AppViewModel
import com.rainyseason.cj.app.OneSignalInitializer
import com.rainyseason.cj.common.AppDnsSelector
import com.rainyseason.cj.common.ConfigManager
import com.rainyseason.cj.common.CoreComponent
import com.rainyseason.cj.common.HasCoreComponent
import com.rainyseason.cj.common.NoopWorker
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.data.KeyValueDatabaseMigrator
import com.rainyseason.cj.data.coc.CoinOmegaCoinInterceptor
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.DebugFlagProvider
import com.rainyseason.cj.featureflag.MainFlagValueProvider
import com.rainyseason.cj.featureflag.RemoteConfigFlagProvider
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.util.ExceptionLoggerTree
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
    lateinit var remoteConfigFlagProvider: RemoteConfigFlagProvider

    @Inject
    lateinit var firebasePerformance: FirebasePerformance

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig

    @Inject // inject for init
    lateinit var configManager: ConfigManager

    @Inject
    lateinit var appDnsSelector: Provider<AppDnsSelector>

    @Inject
    lateinit var commonRepository: Provider<CommonRepository>

    @Inject
    lateinit var keyValueDatabaseMigrator: KeyValueDatabaseMigrator

    @Inject
    lateinit var appViewModelProvider: Provider<AppViewModel>

    @Inject
    lateinit var oneSignalInitializer: OneSignalInitializer

    @Inject
    lateinit var amplitudeInitializer: AmplitudeInitializer

    @Inject
    lateinit var coinOmegaCoinInterceptorProvider: Provider<CoinOmegaCoinInterceptor>

    private lateinit var appComponent: AppComponent

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
        checkFirebaseApp()
        injectIfNecessary()
        keyValueDatabaseMigrator.migrate()
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)

        if (!BuildConfig.IS_PLAY_STORE) {
            Timber.plant(Timber.DebugTree())
            Timber.plant(ExceptionLoggerTree())
        }

        if (!BuildConfig.IS_PLAY_STORE) {
            MainFlagValueProvider.setDelegate(debugFlagProvider.get())
            debugFlagProvider.get().awaitFirstValue()
            if (DebugFlag.USE_REMOTE_CONFIG.isEnable) {
                MainFlagValueProvider.setDelegate(remoteConfigFlagProvider)
            }
        } else {
            MainFlagValueProvider.setDelegate(remoteConfigFlagProvider)
        }
        setAppHash()
        initDns()
        firebasePerformance.isPerformanceCollectionEnabled = BuildConfig.IS_PLAY_STORE

        firebaseAnalytics.setAnalyticsCollectionEnabled(BuildConfig.IS_PLAY_STORE)

        Mavericks.initialize(
            context = this,
            viewModelConfigFactory = MavericksViewModelConfigFactory(
                debugMode = BuildConfig.DEBUG,
                contextOverride = Dispatchers.IO
            )
        )
        registerComponentCallbacks(appViewModelProvider.get())

        enqueueNoopWorker()

        AndroidThreeTen.init(this)

        if (BuildConfig.DEBUG) {
            debugFlagProvider.get().awaitFirstValue()
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(false)
                deleteUnsentReports()
            }
        }
        oneSignalInitializer.invoke()
        coinOmegaCoinInterceptorProvider.get() // register id token listener
        amplitudeInitializer.invoke()
    }

    private fun initDns() {
        // set mode before lookup
        appDnsSelector.get()
    }

    private fun setAppHash() {
        scope.launch {
            val appHash = commonRepository.get().getAppHash()
            FirebaseCrashlytics.getInstance().setUserId(appHash)
            firebaseAnalytics.setUserId(appHash)
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

    companion object {
        var isInTest = false
    }
}
