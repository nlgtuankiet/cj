package com.rainyseason.cj.detail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toRect
import androidx.core.graphics.withClip
import androidx.core.view.updatePadding
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.getColorCompat
import kotlin.math.abs

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class GraphView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
) : View(context, attributeSet), View.OnTouchListener {

    init {
        setOnTouchListener(this)
    }

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
    val clipTop = RectF()
    val clipBottom = RectF()
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


        val spaceStart = paddingStart.toFloat()
        val spaceEnd = paddingEnd.toFloat()
        val spaceTop = paddingTop.toFloat()
        val spaceBottom = paddingBottom.toFloat()

        val widthF = width.toFloat()
        val heightF = height.toFloat()

        val minTime = graphData.first()[0]
        val maxTime = graphData.last()[0]
        val timeRange = maxTime - minTime
        val minPrice = graphData.minOf { it[1] }
        val maxPrice = graphData.maxOf { it[1] }
        val priceRange = maxPrice - minPrice
        val avaWidth = widthF - spaceStart - spaceEnd
        val avaHeight = heightF - spaceTop - paddingBottom
        var started = false
        graphData.forEach { data ->
            val time = data[0]
            val price = data[1]
            val xPercent = (time - minTime) / timeRange
            val x = spaceStart + xPercent * avaWidth
            val yPercent = 1 - (price - minPrice) / priceRange
            val y = spaceTop + yPercent * avaHeight
            if (started) {
                linePath.lineTo(x.toFloat(), y.toFloat())
            } else {
                started = true
                linePath.moveTo(x.toFloat(), y.toFloat())
            }
        }


        val startPricePercent = 1 - (graphData.first()[1] - minPrice) / priceRange
        val middleY = spaceTop + (startPricePercent * avaHeight).toInt()
        clipTop.set(spaceStart, spaceTop, width - spaceEnd, middleY)
        clipBottom.set(spaceStart, middleY, width - spaceEnd, spaceTop + avaHeight.toInt())
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
                greenDrawable.bounds = clipTop.toRect()
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
                redDrawable.bounds = clipBottom.toRect()
                redDrawable.draw(this)
            }
        }


        if (!drawTouchUI) {
            return
        }

        val touchX = touchEvent.x
            .coerceAtLeast(spaceStart)
            .coerceAtMost(widthF - spaceEnd)
        val deltaTouchX = touchX - spaceStart
        val percentTouchX = deltaTouchX / avaWidth
        val touchTime = minTime + percentTouchX * timeRange
        val actualTouchTime = findTouchTime(touchTime)
        val actualTouchXPercent = (actualTouchTime - minTime) / timeRange
        val actualTouchX = spaceStart + actualTouchXPercent * avaWidth

        canvas.drawLine(
            actualTouchX.toFloat(),
            spaceTop,
            actualTouchX.toFloat(),
            heightF - spaceBottom,
            greenLinePaint,
        )
    }

    // TODO use binary search
    fun findTouchTime(touchTime: Double): Double {
        if (graphData.isEmpty()) {
            return touchTime
        }
        return graphData.minByOrNull { abs(touchTime - it[0]) }!![0]
    }

    private var drawTouchUI = false
    private lateinit var touchEvent: MotionEvent

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            drawTouchUI = false
            invalidate()
        }
        if (event.action == MotionEvent.ACTION_MOVE) {
            val time = event.eventTime - event.downTime
            if (time > 350) {
                parent.requestDisallowInterceptTouchEvent(true)
                touchEvent = event
                drawTouchUI = true
                invalidate()
                return true
            }
        }
        return false
    }

}