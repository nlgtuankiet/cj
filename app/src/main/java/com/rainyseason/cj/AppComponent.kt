package com.rainyseason.cj

import android.app.Application
import android.os.Looper
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
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
}


fun checkNotMainThread() {
    check(Looper.myLooper() !== Looper.getMainLooper())
}