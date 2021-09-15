package com.rainyseason.cj

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw


class DrawSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw_sample)
        val textView = findViewById<TextView>(R.id.sample_text)
        textView.doOnPreDraw {
//            Timber.d("verticalPadding: ${textView.verticalPadding()}}}")
        }
    }


}
