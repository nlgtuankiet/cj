package com.rainyseason.cj

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.common.coreComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.system.measureTimeMillis


class DrawSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw_sample)
        val checkBox = findViewById<CheckBox>(R.id.enable_cache)

        findViewById<Button>(R.id.go_button).setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                val time = measureTimeMillis {
                    try {
                        coreComponent.coinGeckoService.getCoinList(forceCache = checkBox.isChecked)
                        Timber.d("request success")
                    } catch (ex: Exception) {
                        Timber.d("request fail")
                        ex.printStackTrace()
                    }
                }
                Timber.d("cache: ${checkBox.isChecked} finish in $time")
            }
        }

    }


}
