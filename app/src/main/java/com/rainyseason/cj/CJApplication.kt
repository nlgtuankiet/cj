package com.rainyseason.cj

import android.app.Application
import com.airbnb.mvrx.Mavericks
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import timber.log.Timber
import javax.inject.Inject

class CJApplication : Application(), HasAndroidInjector {

    @Inject
    @Volatile
    @JvmField
    var androidInjector: DispatchingAndroidInjector<Any>? = null

    private lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.factory().create(this)
        injectIfNecessary()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Mavericks.initialize(this)
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