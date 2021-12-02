package com.rainyseason.cj

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.rainyseason.cj.coinstat.CoinStatFragmentModule
import com.rainyseason.cj.common.AppDnsSelector
import com.rainyseason.cj.common.CoinTickerStorage
import com.rainyseason.cj.common.CoreComponent
import com.rainyseason.cj.common.model.ExchangeJsonAdapter
import com.rainyseason.cj.common.model.ThemeJsonAdapter
import com.rainyseason.cj.common.model.TimeIntervalJsonAdapter
import com.rainyseason.cj.data.CoinHistory
import com.rainyseason.cj.data.CommonStorage
import com.rainyseason.cj.data.ForceCacheInterceptor
import com.rainyseason.cj.data.NetworkUrlLoggerInterceptor
import com.rainyseason.cj.data.NoMustRevalidateInterceptor
import com.rainyseason.cj.data.UserSettingStorage
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.CoinGeckoServiceWrapper
import com.rainyseason.cj.detail.CoinDetailModule
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.setting.SettingFragmentModule
import com.rainyseason.cj.ticker.CoinTickerSettingActivityModule
import com.rainyseason.cj.tracking.AppTracker
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.watch.WatchListFragmentModule
import com.rainyseason.cj.widget.watch.Watch
import com.rainyseason.cj.widget.watch.WatchClickActionJsonAdapter
import com.rainyseason.cj.widget.watch.WatchPreviewFragmentModule
import com.rainyseason.cj.widget.watch.WatchSettingActivityModule
import com.rainyseason.cj.widget.watch.WatchWidgetLayoutJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.addAdapter
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Cache
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        MainActivityModule::class,
        CoinTickerSettingActivityModule::class,
        WatchListFragmentModule::class,
        CoinDetailModule::class,
        AppProvides::class,
        AppBinds::class,
        CoinStatFragmentModule::class,
        SettingFragmentModule::class,
        WatchSettingActivityModule::class,
        WatchPreviewFragmentModule::class,
    ]
)
@Singleton
interface AppComponent : AndroidInjector<CJApplication>, CoreComponent {

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
        ): AppComponent
    }
}

@Module
interface AppBinds {
    @Binds
    fun tracker(appTracker: AppTracker): Tracker
}

@Module
object AppProvides {

    @Provides
    @Singleton
    fun firebasePerformance(): FirebasePerformance {
        return FirebasePerformance.getInstance()
    }

    @Provides
    @Singleton
    fun firebaseRemoteConfig(): FirebaseRemoteConfig {
        return Firebase.remoteConfig
    }

    @Provides
    @Singleton
    fun provideBaseClientBuilder(
        forceCacheInterceptor: ForceCacheInterceptor,
        networkUrlLoggerInterceptor: NetworkUrlLoggerInterceptor,
        noMustRevalidateInterceptor: NoMustRevalidateInterceptor,
        appDnsSelector: AppDnsSelector,
    ): OkHttpClient.Builder {
        checkNotMainThread()
        val builder = OkHttpClient.Builder()
        builder.addNetworkInterceptor(noMustRevalidateInterceptor)
        builder.addInterceptor(forceCacheInterceptor)
        builder.dns(appDnsSelector)
        builder.connectTimeout(1, TimeUnit.MINUTES)
        builder.readTimeout(1, TimeUnit.MINUTES)
        builder.writeTimeout(1, TimeUnit.MINUTES)
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }
            logging.level = HttpLoggingInterceptor.Level.BODY

            val networkLogging = HttpLoggingInterceptor { message ->
                Timber.tag("OkHttpN").d(message)
            }
            networkLogging.level = HttpLoggingInterceptor.Level.HEADERS

            if (DebugFlag.SHOW_HTTP_LOG.isEnable) {
                builder.addInterceptor(logging)
                if (DebugFlag.SHOW_NETWORK_LOG.isEnable) {
                    builder.addNetworkInterceptor(networkLogging)
                    builder.addNetworkInterceptor(networkUrlLoggerInterceptor)
                }
            }
        }
        return builder
    }

    @Provides
    @Singleton
    fun firebaseCrashlytic(): FirebaseCrashlytics {
        return FirebaseCrashlytics.getInstance()
    }

    @Provides
    @Singleton
    fun firebaseAnalytic(context: Context): FirebaseAnalytics {
        return FirebaseAnalytics.getInstance(context)
    }

    @Qualifier
    annotation class CoinGecko

    @Provides
    @CoinGecko
    @Singleton
    fun coinGeckoCallFactory(
        context: Context,
        clientProvider: Provider<OkHttpClient>,
    ): Call.Factory {
        val coinGeckoClient: OkHttpClient by lazy {
            clientProvider.get().newBuilder()
                .cache(Cache(File(context.cacheDir, "coin_gecko_api"), 50L * 1024 * 1024))
                .build()
        }
        return Call.Factory { coinGeckoClient.newCall(it) }
    }

    @Provides
    @Singleton
    fun provideBaseClient(builder: OkHttpClient.Builder): OkHttpClient {
        checkNotMainThread()
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideCallFactory(clientProvider: Provider<OkHttpClient>): Call.Factory {
        return Call.Factory { request -> clientProvider.get().newCall(request) }
    }

    @ExperimentalStdlibApi
    @Provides
    @Singleton
    fun moshi(): Moshi {
        return Moshi.Builder()
            .addAdapter(TimeIntervalJsonAdapter)
            .addAdapter(WatchWidgetLayoutJsonAdapter)
            .addAdapter(ThemeJsonAdapter)
            .addAdapter(ExchangeJsonAdapter)
            .addAdapter(WatchClickActionJsonAdapter)
            .build()
    }

    @Provides
    @Singleton
    fun provideCoinGeckoService(
        moshi: Moshi,
        @CoinGecko
        callFactory: Call.Factory,
    ): CoinGeckoService {
        val service = Retrofit.Builder()
            .baseUrl(CoinGeckoService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory(callFactory)
            .build()
            .create(CoinGeckoService::class.java)

        return CoinGeckoServiceWrapper(service)
    }

    @Provides
    fun context(application: Application): Context {
        return application
    }

    @Provides
    @Singleton
    fun widgetManager(context: Context): AppWidgetManager {
        return AppWidgetManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideCoinTickerStorage(context: Context): CoinTickerStorage {
        val pref = createStorage(context, "coin_ticker_storage")
        return CoinTickerStorage(pref)
    }

    private fun createStorage(context: Context, name: String): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                context.preferencesDataStoreFile(name)
            }
        )
    }

    @Provides
    @Singleton
    fun provideUserSettingStorage(context: Context): UserSettingStorage {
        val pref = createStorage(context, "user_setting_storage")
        return UserSettingStorage(pref)
    }

    @Provides
    @CommonStorage
    @Singleton
    fun provideCommonStorage(context: Context): DataStore<Preferences> {
        return createStorage(context, "common")
    }

    @Provides
    @Watch
    @Singleton
    fun provideWatchStorage(context: Context): DataStore<Preferences> {
        return createStorage(context, "watch_storage")
    }

    @Provides
    @CoinHistory
    @Singleton
    fun provideCoinHistoryStorage(context: Context): DataStore<Preferences> {
        return createStorage(context, "coin_history")
    }

    @Provides
    @Singleton
    fun workManager(
        context: Context,
        factory: AppWorkerFactory,
    ): WorkManager {
        val config = Configuration.Builder()
            .setWorkerFactory(factory)
            .apply {
                if (BuildConfig.IS_PLAY_STORE) {
                    setMinimumLoggingLevel(Int.MAX_VALUE)
                } else {
                    setMinimumLoggingLevel(Log.VERBOSE)
                }
            }
            .build()
        WorkManager.initialize(context, config)
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun powerManager(context: Context): PowerManager {
        return context.getSystemService()!!
    }
}

fun checkNotMainThread() {
    check(Looper.myLooper() !== Looper.getMainLooper())
}
