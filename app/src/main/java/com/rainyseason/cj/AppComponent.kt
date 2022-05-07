package com.rainyseason.cj

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.util.Log.VERBOSE
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.room.Room
import androidx.work.Configuration
import androidx.work.WorkManager
import com.amplitude.api.AmplitudeClient
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.ihsanbal.logging.Level
import com.ihsanbal.logging.LoggingInterceptor
import com.rainyseason.cj.app.AmplitudeInitializer
import com.rainyseason.cj.app.FirebaseAnalyticInitializer
import com.rainyseason.cj.chat.admin.ChatAdminActivityModule
import com.rainyseason.cj.chat.history.ChatHistoryModule
import com.rainyseason.cj.chat.list.ChatListModule
import com.rainyseason.cj.chat.login.ChatLoginModule
import com.rainyseason.cj.coinselect.CoinSelectFragmentModule
import com.rainyseason.cj.coinstat.CoinStatFragmentModule
import com.rainyseason.cj.common.AppDnsSelector
import com.rainyseason.cj.common.CoreComponent
import com.rainyseason.cj.common.home.AddWidgetTutorialFragmentModule
import com.rainyseason.cj.common.model.BackendJsonAdapter
import com.rainyseason.cj.common.model.ThemeJsonAdapter
import com.rainyseason.cj.common.model.TimeIntervalJsonAdapter
import com.rainyseason.cj.data.CoinHistoryEntry
import com.rainyseason.cj.data.ForceCacheInterceptor
import com.rainyseason.cj.data.NoMustRevalidateInterceptor
import com.rainyseason.cj.data.UrlLoggerInterceptor
import com.rainyseason.cj.data.binance.BinanceService
import com.rainyseason.cj.data.binance.BinanceServiceWrapper
import com.rainyseason.cj.data.cmc.CmcService
import com.rainyseason.cj.data.coc.CocAuthenticator
import com.rainyseason.cj.data.coc.CoinOmegaCoinInterceptor
import com.rainyseason.cj.data.coc.CoinOmegaCoinService
import com.rainyseason.cj.data.coinbase.CoinbaseService
import com.rainyseason.cj.data.coinbase.CoinbaseServiceWrapper
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.coingecko.CoinGeckoServiceWrapper
import com.rainyseason.cj.data.database.kv.KeyValueDao
import com.rainyseason.cj.data.database.kv.KeyValueDatabase
import com.rainyseason.cj.data.dexscreener.DexScreenerService
import com.rainyseason.cj.data.dexscreener.DexScreenerServiceWrapper
import com.rainyseason.cj.data.ftx.FtxService
import com.rainyseason.cj.data.interceptor.synchronized
import com.rainyseason.cj.data.kraken.Kraken
import com.rainyseason.cj.data.kraken.KrakenInterceptor
import com.rainyseason.cj.data.kraken.KrakenService
import com.rainyseason.cj.data.luno.LunoService
import com.rainyseason.cj.data.luno.LunoWebService
import com.rainyseason.cj.data.luno.LunoWebServiceWrapper
import com.rainyseason.cj.detail.CoinDetailModule
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.setting.SettingFragmentModule
import com.rainyseason.cj.ticker.CoinTickerLayoutJsonAdapter
import com.rainyseason.cj.ticker.CoinTickerSettingActivityModule
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewFragmentModule
import com.rainyseason.cj.tracking.AppTracker
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.watch.WatchListFragmentModule
import com.rainyseason.cj.widget.manage.ManageWidgetFragmentModule
import com.rainyseason.cj.widget.watch.WatchClickActionJsonAdapter
import com.rainyseason.cj.widget.watch.WatchPreviewFragmentModule
import com.rainyseason.cj.widget.watch.WatchSettingActivityModule
import com.rainyseason.cj.widget.watch.WatchWidgetLayoutJsonAdapter
import com.rainyseason.cj.widget.watch.fullsize.WatchWidgetServiceModule
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
    modules = [AllModules::class]
)
@Singleton
interface AppComponent : AndroidInjector<CJApplication>, CoreComponent {

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance context: Context,
        ): AppComponent
    }
}

@Module(
    includes = [
        AndroidSupportInjectionModule::class,
        MainActivityModule::class,
        CoinTickerSettingActivityModule::class,
        WatchListFragmentModule::class,
        CoinDetailModule::class,
        AppProvides::class,
        AppBinds::class,
        DrawSampleActivityModule::class,
        CoinStatFragmentModule::class,
        SettingFragmentModule::class,
        WatchWidgetServiceModule::class,
        WatchSettingActivityModule::class,
        WatchPreviewFragmentModule::class,
        ManageWidgetFragmentModule::class,
        ChatHistoryModule::class,
        ChatListModule::class,
        ChatLoginModule::class,
        ChatAdminActivityModule::class,
        CoinTickerPreviewFragmentModule::class,
        AddWidgetTutorialFragmentModule::class,
        CoinSelectFragmentModule::class,
    ]
)
interface AllModules

@Module
interface AppBinds {
    @Binds
    fun tracker(appTracker: AppTracker): Tracker
}

@Module
object AppProvides {

    @Provides
    @Singleton
    fun notificationManager(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context)
    }

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
    fun firebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    @Provides
    @Singleton
    fun firebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun appScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @Provides
    @Singleton
    fun provideBaseClientBuilder(
        forceCacheInterceptor: ForceCacheInterceptor,
        urlLoggerInterceptor: UrlLoggerInterceptor,
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
            builder.addInterceptor(urlLoggerInterceptor)

            if (DebugFlag.SHOW_HTTP_LOG.isEnable) {
                if (DebugFlag.SHOW_HTTP_LOG_JSON.isEnable) {
                    val logging = LoggingInterceptor.Builder()
                        .setLevel(Level.BODY)
                        .tag("OkHttp")
                        .log(VERBOSE)
                        .build()
                    builder.addInterceptor(logging.synchronized())
                } else {
                    val logging = HttpLoggingInterceptor { message ->
                        Timber.tag("OkHttp").d(message)
                    }
                    logging.level = HttpLoggingInterceptor.Level.HEADERS
                    builder.addInterceptor(logging.synchronized())
                }

                if (DebugFlag.SHOW_NETWORK_LOG.isEnable) {
                    val networkLogging = HttpLoggingInterceptor { message ->
                        Timber.tag("OkHttpN").d(message)
                    }
                    networkLogging.level = HttpLoggingInterceptor.Level.HEADERS
                    builder.addNetworkInterceptor(networkLogging.synchronized())
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
    fun firebaseAnalytic(
        firebaseAnalyticInitializer: FirebaseAnalyticInitializer
    ): FirebaseAnalytics {
        return firebaseAnalyticInitializer.initAndGetInstance()
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
    @Kraken
    @Singleton
    fun krakenClient(
        clientProvider: Provider<OkHttpClient>,
        krakenInterceptor: KrakenInterceptor,
    ): Call.Factory {
        val krakenClient: OkHttpClient by lazy {
            clientProvider.get().newBuilder()
                .addNetworkInterceptor(krakenInterceptor)
                .build()
        }
        return Call.Factory { krakenClient.newCall(it) }
    }

    @Provides
    @Singleton
    fun provideBaseClient(builder: OkHttpClient.Builder): OkHttpClient {
        checkNotMainThread()
        return builder.build()
    }

    @Provides
    @Singleton
    fun amplitude(
        amplitudeInitializer: AmplitudeInitializer
    ): AmplitudeClient {
        return amplitudeInitializer.initAndGetInstance()
    }

    @Provides
    @Singleton
    fun provideCallFactory(clientProvider: Provider<OkHttpClient>): Call.Factory {
        return Call.Factory { request -> clientProvider.get().newCall(request) }
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Provides
    @Singleton
    fun moshi(): Moshi {
        return Moshi.Builder()
            .add(
                String::class.java,
                CoinHistoryEntry.NullToCoinGeckoUrl::class.java,
                CoinHistoryEntry.NullIconUrlToCoinGeckoUrlAdapter
            )
            .addAdapter(TimeIntervalJsonAdapter)
            .addAdapter(WatchWidgetLayoutJsonAdapter)
            .addAdapter(CoinTickerLayoutJsonAdapter)
            .addAdapter(ThemeJsonAdapter)
            .addAdapter(BackendJsonAdapter)
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
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .callFactory(callFactory)
            .build()
            .create(CoinGeckoService::class.java)

        return CoinGeckoServiceWrapper(service)
    }

    @Provides
    @Singleton
    fun cocService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
        coinOmegaCoinInterceptor: CoinOmegaCoinInterceptor,
        cocAuthenticator: CocAuthenticator,
    ): CoinOmegaCoinService {
        val client: OkHttpClient by lazy {
            clientProvider.get().newBuilder()
                .addInterceptor(coinOmegaCoinInterceptor)
                .authenticator(cocAuthenticator)
                .build()
        }
        return Retrofit.Builder()
            .baseUrl(BuildConfig.COC_HOST)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { client.newCall(it) }
            .build()
            .create(CoinOmegaCoinService::class.java)
    }

    @Provides
    @Singleton
    fun provideBinanceService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
    ): BinanceService {
        val service = Retrofit.Builder()
            .baseUrl(BinanceService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { clientProvider.get().newCall(it) }
            .build()
            .create(BinanceService::class.java)
        return BinanceServiceWrapper(service)
    }

    @Provides
    @Singleton
    fun provideCoinBaseService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
    ): CoinbaseService {
        val service = Retrofit.Builder()
            .baseUrl(CoinbaseService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { clientProvider.get().newCall(it) }
            .build()
            .create(CoinbaseService::class.java)
        return CoinbaseServiceWrapper(service)
    }

    @Provides
    @Singleton
    fun provideFtxService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
    ): FtxService {
        return Retrofit.Builder()
            .baseUrl(FtxService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { clientProvider.get().newCall(it) }
            .build()
            .create(FtxService::class.java)
    }

    @Provides
    @Singleton
    fun provideCmcService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
    ): CmcService {
        return Retrofit.Builder()
            .baseUrl(CmcService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { clientProvider.get().newCall(it) }
            .build()
            .create(CmcService::class.java)
    }

    @Provides
    @Singleton
    fun provideLunoWebService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
    ): LunoWebService {
        val service = Retrofit.Builder()
            .baseUrl(LunoWebService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { clientProvider.get().newCall(it) }
            .build()
            .create(LunoWebService::class.java)
        return LunoWebServiceWrapper(service)
    }

    @Provides
    @Singleton
    fun provideDexScreenerService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
    ): DexScreenerService {
        val service = Retrofit.Builder()
            .baseUrl(DexScreenerService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { clientProvider.get().newCall(it) }
            .build()
            .create(DexScreenerService::class.java)
        return DexScreenerServiceWrapper(service)
    }

    @Provides
    @Singleton
    fun provideLunoService(
        moshi: Moshi,
        clientProvider: Provider<OkHttpClient>,
    ): LunoService {
        return Retrofit.Builder()
            .baseUrl(LunoService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory { clientProvider.get().newCall(it) }
            .build()
            .create(LunoService::class.java)
    }

    @Provides
    @Singleton
    fun provideKrakenService(
        moshi: Moshi,
        @Kraken
        callFactory: Call.Factory,
    ): KrakenService {
        return Retrofit.Builder()
            .baseUrl(KrakenService.BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory(callFactory)
            .build()
            .create(KrakenService::class.java)
    }

    @Provides
    @Singleton
    fun provideKeyValueDatabase(context: Context): KeyValueDatabase {
        checkNotMainThread()
        return Room.databaseBuilder(context, KeyValueDatabase::class.java, "key_value")
            .build()
    }

    @Provides
    @Singleton
    fun provideEntryDao(db: KeyValueDatabase): KeyValueDao {
        return db.entryDao()
    }

    @Provides
    @Singleton
    fun widgetManager(context: Context): AppWidgetManager {
        return AppWidgetManager.getInstance(context)
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
        try {
            WorkManager.initialize(context, config)
        } catch (ex: Exception) {
            if (!BuildConfig.DEBUG) {
                throw ex
            }
        }

        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun powerManager(context: Context): PowerManager {
        return context.getSystemService()!!
    }
}

fun checkNotMainThread() {
    if (BuildConfig.DEBUG) {
        check(Looper.myLooper() !== Looper.getMainLooper()) {
            "Invoke on main thread"
        }
    }
}
