package com.rainyseason.cj.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat
import androidx.core.graphics.withClip
import com.rainyseason.cj.R
import com.rainyseason.cj.common.model.Theme
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class GraphRenderer @Inject constructor(
    private val renderUtil: WidgetRenderUtil,
) {
    fun createGraphBitmap(
        context: Context,
        theme: Theme,
        inputWidth: Float,
        inputHeight: Float,
        data: List<List<Double>>,
        isPositiveOverride: Boolean? = null
    ): Bitmap {
        // [0] timestamp
        // [1] price
        val width = inputWidth.coerceAtLeast(1f)
        val height = inputHeight.coerceAtLeast(1f)
        val bitmap = Bitmap.createBitmap(
            width.toInt(),
            height.toInt(),
            Bitmap.Config.ARGB_8888
        )
        if (data.size <= 2) {
            return bitmap
        }
        val isPositive = isPositiveOverride ?: (data.first()[1] < data.last()[1])
        val canvas = Canvas(bitmap)
        val paint = Paint()
        paint.strokeWidth = context.dpToPxF(1.5f)
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND
        paint.color = renderUtil.getTickerLineColor(theme, isPositive)
        val minTime = data.minOf { it[0] }
        val maxTime = data.maxOf { it[0] }
        val timeInterval = maxTime - minTime
        val minPrice = data.minOf { it[1] }
        val maxPrice = data.maxOf { it[1] }
        val priceInterval = maxPrice - minPrice
        val path = Path()

        var minY = height

        var started = false
        data.forEach { point ->
            val currentPointX = ((point[0] - minTime) / timeInterval * width).toFloat()
            val currentPointY = (height - (point[1] - minPrice) / priceInterval * height).toFloat()
            minY = min(minY, currentPointY)
            if (started) {
                path.lineTo(currentPointX, currentPointY)
            } else {
                started = true
                path.moveTo(currentPointX, currentPointY)
            }
        }
        canvas.drawPath(path, paint)

        // clip
        path.lineTo(width, height)
        path.lineTo(0f, height)
        path.close()

        val drawableRes = if (isPositive) {
            R.drawable.graph_background_green
        } else {
            R.drawable.graph_background_red
        }
        val gradientDrawable = ContextCompat.getDrawable(context, drawableRes)!!

        canvas.withClip(path) {
            gradientDrawable.setBounds(0, 0, width.toInt(), height.toInt())
            gradientDrawable.draw(this)
        }

        return bitmap
    }
}
