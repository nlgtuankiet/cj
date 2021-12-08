package com.rainyseason.cj

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.common.model.Backend
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        setContentView(R.layout.activity_draw_sample)
        val checkBox = findViewById<CheckBox>(R.id.enable_cache)

        findViewById<Button>(R.id.go_button).setOnClickListener {
            val map = mutableMapOf<Backend, List<String>>()
            map
        }
    }
}
