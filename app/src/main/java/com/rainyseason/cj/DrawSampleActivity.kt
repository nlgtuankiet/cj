package com.rainyseason.cj

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.amplitude.api.AmplitudeClient
import com.rainyseason.cj.data.cmc.CmcService
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import javax.inject.Inject

@Module
interface DrawSampleActivityModule {
    @ContributesAndroidInjector
    fun activity(): DrawSampleActivity
}

class DrawSampleActivity : AppCompatActivity() {

    @Inject
    lateinit var cmcService: CmcService

    @Inject
    lateinit var amplitudeClient: AmplitudeClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        setContentView(R.layout.activity_draw_sample)
        if (!BuildConfig.DEBUG) {
            finish()
            return
        }
        amplitudeClient.logEvent("hello_world")
    }
}
