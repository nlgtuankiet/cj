package com.rainyseason.cj.detail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.withClip
import androidx.core.view.updatePadding
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.getColorCompat

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class GraphView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : View(context, attributeSet) {

    private val heightDp = 240
    private var graphData: List<List<Double>> = emptyList()

    private val greenLineColor = context.getColorCompat(R.color.ticket_line_green)
    private val greenLineRed = context.getColorCompat(R.color.ticket_line_red)

    val greenLinePaint = Paint().apply {
        color = greenLineColor
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = context.dpToPx(2).toFloat()
    }

    val redLinePaint = Paint().apply {
        color = greenLineRed
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = context.dpToPx(2).toFloat()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), context.dpToPx(heightDp))
        val dp16 = context.dpToPx(16)
        updatePadding(left = dp16, right = dp16, top = dp16)
    }


    @ModelProp
    fun setGraph(data: List<List<Double>>) {
        graphData = data
        invalidate()
    }

    val linePath = Path()
    val lineTop = Path()
    val lineBottom = Path()
    val clipTop = Rect()
    val clipBottom = Rect()
    val greenDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0, 0))
    val redDrawable = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0, 0))
    val lineBackgroundColorGreen = context.getColorCompat(R.color.ticket_line_green_background)
    val lineBackgroundColorRed = context.getColorCompat(R.color.ticket_line_red_background)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (graphData.isEmpty()) {
            return
        }
        linePath.reset()
        val widthF = width.toFloat()
        val heightF = height.toFloat()

        val minTime = graphData.first()[0]
        val maxTime = graphData.last()[0]
        val timeRange = maxTime - minTime
        val minPrice = graphData.minOf { it[1] }
        val maxPrice = graphData.maxOf { it[1] }
        val priceRange = maxPrice - minPrice
        val avaWidth = widthF - paddingStart - paddingEnd
        val avaHeight = heightF - paddingTop - paddingBottom
        var started = false
        graphData.forEach { data ->
            val time = data[0]
            val price = data[1]
            val xPercent = (time - minTime) / timeRange
            val x = paddingStart + xPercent * avaWidth
            val yPercent = 1 - (price - minPrice) / priceRange
            val y = paddingTop + yPercent * avaHeight
            if (started) {
                linePath.lineTo(x.toFloat(), y.toFloat())
            } else {
                started = true
                linePath.moveTo(x.toFloat(), y.toFloat())
            }
        }


        val startPricePercent = 1 - (graphData.first()[1] - minPrice) / priceRange
        val middleY = paddingTop + (startPricePercent * avaHeight).toInt()
        clipTop.set(paddingStart, paddingTop, width - paddingEnd, middleY)
        clipBottom.set(paddingStart, middleY, width - paddingEnd, paddingTop + avaHeight.toInt())
        println("clipTop: $clipTop clipBottom: $clipBottom, startPricePercent: $startPricePercent")

        val greenAlpha = (0.5 * startPricePercent).coerceAtLeast(0.25) * 255
        val greenBackground = Color.argb(
            greenAlpha.toInt(),
            Color.red(lineBackgroundColorGreen),
            Color.green(lineBackgroundColorGreen),
            Color.blue(lineBackgroundColorGreen),
        )

        lineBottom.set(linePath)
        lineBottom.lineTo(clipTop.right.toFloat(), clipTop.bottom.toFloat()) // end bottom
        lineBottom.lineTo(clipTop.left.toFloat(), clipTop.bottom.toFloat()) // start bottom
        lineBottom.close()
        canvas.withClip(clipTop) {
            drawPath(linePath, greenLinePaint)
            withClip(lineBottom) {
                greenDrawable.colors = intArrayOf(greenBackground, 0)
                greenDrawable.bounds = clipTop
                greenDrawable.draw(this)
            }
        }

        val redAlpha = (0.5 * (1 - startPricePercent)).coerceAtLeast(0.25) * 255
        val redBackground = Color.argb(
            redAlpha.toInt(),
            Color.red(lineBackgroundColorRed),
            Color.green(lineBackgroundColorRed),
            Color.blue(lineBackgroundColorRed),
        )

        lineTop.set(linePath)
        lineTop.lineTo(clipBottom.right.toFloat(), clipBottom.bottom.toFloat()) // end bottom
        lineTop.lineTo(clipBottom.left.toFloat(), clipBottom.bottom.toFloat()) // start bottom
        lineTop.close()
        canvas.withClip(clipBottom) {
            drawPath(linePath, redLinePaint)
            withClip(lineBottom) {
                redDrawable.colors = intArrayOf(redBackground, 0)
                redDrawable.bounds = clipBottom
                redDrawable.draw(this)
            }
        }


    }

}