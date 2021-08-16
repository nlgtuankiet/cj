package com.rainyseason.cj

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.Configuration
import androidx.work.WorkManager
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.ticker.CoinTickerHandlerModule
import com.rainyseason.cj.ticker.CoinTickerProviderModule
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
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Provider
import javax.inject.Singleton

@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        MainActivityModule::class,
        CoinTickerSettingActivityModule::class,
        CoinTickerProviderModule::class,
        CoinTickerHandlerModule::class,
        AppModule::class
    ]
)
@Singleton
interface AppComponent : AndroidInjector<CJApplication> {

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
        return OkHttpClient.Builder()
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
    fun providePrefs(context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = null,
            migrations = emptyList(),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = {
                context.preferencesDataStoreFile("settings")
            }
        )
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