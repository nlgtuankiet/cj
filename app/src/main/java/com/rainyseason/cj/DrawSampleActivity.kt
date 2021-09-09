package com.rainyseason.cj

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.common.drawLinearGradient

class DrawSampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw_sample)
        val container = findViewById<ImageView>(R.id.container)
        container.setImageBitmap(drawLinearGradient(this))
    }
}