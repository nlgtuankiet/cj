package com.rainyseason.cj

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.withClip
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.lifecycleScope
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.dpToPxF
import com.rainyseason.cj.common.drawLinearGradient
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.samplePrice30Day
import com.rainyseason.cj.data.coingecko.MarketChartResponseJsonAdapter
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.min

class DrawSampleActivity : AppCompatActivity() {

    lateinit var container: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_draw_sample)
        container = findViewById(R.id.container)
        drawGraph()
    }


    fun drawGraph() {
        container.setImageBitmap(drawLinearGradient(this))

        lifecycleScope.launch {
            val response = MarketChartResponseJsonAdapter(coreComponent.moshi).fromJson(
                samplePrice30Day
            )!!
            container.doOnPreDraw {
                val bitmap = createGraphBitmap(
                    context = this@DrawSampleActivity,
                    width = container.width,
                    height = container.height,
                    isPositive = true,
                    data = response.prices.filter { it.size == 2 }
                )
                container.setImageBitmap(bitmap)
            }

        }
    }

    private fun createGraphBitmap(
        context: Context,
        width: Int,
        height: Int,
        isPositive: Boolean,
        data: List<List<Double>>,
    ): Bitmap {
        // [0] timestamp
        // [1] price
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.strokeWidth = context.dpToPxF(1.5f)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = if (isPositive) {
            context.getColorCompat(R.color.ticket_line_green)
        } else {
            context.getColorCompat(R.color.ticket_line_red)
        }
        val minTime = data.minOf { it[0] }
        val maxTime = data.maxOf { it[0] }
        val timeInterval = maxTime - minTime
        val minPrice = data.minOf { it[1] }
        val maxPrice = data.maxOf { it[1] }
        val priceInterval = maxPrice - minPrice
        val path = Path()

        var minY = height.toFloat()

        var started = false
        data.forEach { point ->
            val currentPointX = ((point[0] - minTime) / timeInterval * width).toFloat()
            val currentPointY = (height - (point[1] - minPrice) / priceInterval * height).toFloat()
            minY = min(minY, currentPointY)
            if (started) {
                Timber.d("line to ${currentPointX.toInt()} ${currentPointY.toInt()}")
                path.lineTo(currentPointX, currentPointY)
            } else {
                started = true
                path.moveTo(currentPointX, currentPointY)
                Timber.d("move to ${currentPointX.toInt()} ${currentPointY.toInt()}")
            }
        }
        canvas.drawPath(path, paint)

        // clip
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()

        val gradientColor = if (isPositive) {
            context.getColorCompat(R.color.ticket_line_green_background)
        } else {
            context.getColorCompat(R.color.ticket_line_red_background)
        }

        canvas.withClip(path) {
            val gradient = LinearGradient(
                width / 2f, minY, width / 2f, height.toFloat(),
                gradientColor,
                context.getColorCompat(android.R.color.transparent),
                Shader.TileMode.CLAMP
            )
            val gPaint = Paint()
            gPaint.isDither = true
            gPaint.shader = gradient
            drawRect(0f, 0f, width.toFloat(), height.toFloat(), gPaint)
        }

        return bitmap

    }
}