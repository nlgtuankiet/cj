package com.rainyseason.cj

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.Configuration
import androidx.work.WorkManager
import com.rainyseason.cj.common.CoinTickerStorage
import com.rainyseason.cj.common.CoreComponent
import com.rainyseason.cj.data.UserSettingStorage
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.ticker.CoinTickerSettingActivityModule
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import timber.log.Timber
import javax.inject.Provider
import javax.inject.Singleton


@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        MainActivityModule::class,
        CoinTickerSettingActivityModule::class,
        AppModule::class
    ]
)
@Singleton
interface AppComponent : AndroidInjector<CJApplication>, CoreComponent {


    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application
        ): AppComponent
    }
}


@Module
object AppModule {

    @Provides
    @Singleton
    fun provideBaseClientBuilder(): OkHttpClient.Builder {
        checkNotMainThread()
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor { message -> Timber.tag("OkHttp").d(message) }
            logging.level = HttpLoggingInterceptor.Level.BODY
            builder.addInterceptor(logging)
        }
        return builder
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

    @Provides
    @Singleton
    fun moshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    @Singleton
    fun provideCoinGeckoService(
        moshi: Moshi,
        callFactory: Call.Factory,
    ): CoinGeckoService {
        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .callFactory(callFactory)
            .build()
            .create(CoinGeckoService::class.java)
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
        val pref = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                context.preferencesDataStoreFile("coin_ticker_storage")
            }
        )
        return CoinTickerStorage(pref)
    }

    @Provides
    @Singleton
    fun provideUserSettingStorage(context: Context): UserSettingStorage {
        val pref = PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                context.preferencesDataStoreFile("user_setting_storage")
            }
        )
        return UserSettingStorage(pref)
    }

    @Provides
    @Singleton
    fun workManager(
        context: Context,
        factory: AppWorkerFactory,
    ): WorkManager {
        val config = Configuration.Builder().setWorkerFactory(factory).build()
        WorkManager.initialize(context, config)
        return WorkManager.getInstance(context)
    }
}


fun checkNotMainThread() {
    check(Looper.myLooper() !== Looper.getMainLooper())
}