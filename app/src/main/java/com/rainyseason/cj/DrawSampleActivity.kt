package com.rainyseason.cj

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider


class DrawSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw_sample)
        val slider = findViewById<Slider>(R.id.slider)
        slider.valueFrom = 0f
        slider.valueTo = 100f
        slider.stepSize = 5f
        slider.addOnChangeListener { slider, value, fromUser ->
            if (fromUser) {
                println("value: $value")
            }
        }
    }


}
