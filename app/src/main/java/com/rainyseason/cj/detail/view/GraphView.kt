package com.rainyseason.cj.detail.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toRect
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import androidx.core.view.updatePadding
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.google.android.material.textview.MaterialTextView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.clearHapticFeedback
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.dpToPxF
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.hapticFeedbackIfChanged
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
        updatePadding(left = dp16, right = dp16, top = dp16, bottom = dp16)
    }

    @ModelProp
    fun setGraph(data: List<List<Double>>) {
        graphData = data
        invalidate()
    }

    @set:CallbackProp
    var onDataTouchListener: ((Int?) -> Unit)? = null

    private val startPriceMarginStart = context.dpToPx(8)
    private val startPriceTextView = MaterialTextView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        setTextColor(Color.WHITE)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setBackgroundResource(R.drawable.detail_graph_start_price_background)
        updatePadding(
            left = context.dpToPx(8),
            top = context.dpToPx(2),
            right = context.dpToPx(8),
            bottom = context.dpToPx(2),
        )
    }

    @ModelProp
    fun setStartPrice(value: String) {
        startPriceTextView.text = value
        val specs = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        startPriceTextView.measure(specs, specs)
        startPriceTextView.layout(
            0,
            0,
            startPriceTextView.measuredWidth,
            startPriceTextView.measuredHeight
        )
    }

    val tmpPoint = PointF()

    val linePath = Path()
    val lineTop = Path()
    val lineBottom = Path()
    val clipTop = RectF()
    val clipBottom = RectF()
    val greenDrawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0, 0))
    val redDrawable = GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, intArrayOf(0, 0))
    val lineBackgroundColorGreen = context.getColorCompat(R.color.ticket_line_green_background)
    val lineBackgroundColorRed = context.getColorCompat(R.color.ticket_line_red_background)

    // line
    private val dashPaint = Paint().apply {
        color = context.getColorCompat(R.color.gray_500)
        style = Paint.Style.STROKE
        strokeWidth = context.dpToPxF(1f)
        pathEffect = DashPathEffect(
            floatArrayOf(
                context.dpToPxF(1f),
                context.dpToPxF(4f),
            ),
            0f,
        )
    }

    // circle
    private val innerCircleRadius = context.dpToPxF(4f)
    private val innerCirclePaint = Paint().apply {
        color = context.getColorCompat(R.color.gray_500)
        style = Paint.Style.FILL
    }

    private val outterCircleRadius = context.dpToPxF(8f)
    private val outterCirclePaint = Paint().apply {
        color = context.getColorCompat(R.color.gray_700)
        style = Paint.Style.FILL
    }

    private fun calPoint(
        outPoint: PointF,
        data: List<Double>,
        minTime: Double,
        timeRange: Double,
        spaceStart: Float,
        avaWidth: Float,
        minPrice: Double,
        priceRange: Double,
        spaceTop: Float,
        avaHeight: Float,
    ) {
        val time = data[0]
        val price = data[1]
        val xPercent = (time - minTime) / timeRange
        val x = spaceStart + xPercent * avaWidth
        val yPercent = 1 - (price - minPrice) / priceRange
        val y = spaceTop + yPercent * avaHeight
        outPoint.set(x.toFloat(), y.toFloat())
    }

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
        val avaHeight = heightF - spaceTop - spaceBottom

        val drawEnd = widthF - spaceEnd
        val drawBottom = heightF - spaceBottom

        var started = false
        graphData.forEach { data ->
            calPoint(
                outPoint = tmpPoint,
                data = data,
                minTime = minTime,
                timeRange = timeRange,
                spaceStart = spaceStart,
                avaWidth = avaWidth,
                minPrice = minPrice,
                priceRange = priceRange,
                spaceTop = spaceTop,
                avaHeight = avaHeight,
            )
            if (started) {
                linePath.lineTo(tmpPoint.x, tmpPoint.y)
            } else {
                started = true
                linePath.moveTo(tmpPoint.x, tmpPoint.y)
            }
        }

        val startPricePercent = 1 - (graphData.first()[1] - minPrice) / priceRange
        val middleY = spaceTop + (startPricePercent * avaHeight).toInt()
        clipTop.set(spaceStart, spaceTop, width - spaceEnd, middleY)
        clipBottom.set(spaceStart, middleY, width - spaceEnd, drawBottom)
        println("clipTop: $clipTop clipBottom: $clipBottom, startPricePercent: $startPricePercent")

        val greenAlpha = (0.5 * startPricePercent).coerceAtLeast(0.25) * 255
        val greenBackground = Color.argb(
            greenAlpha.toInt(),
            Color.red(lineBackgroundColorGreen),
            Color.green(lineBackgroundColorGreen),
            Color.blue(lineBackgroundColorGreen),
        )

        lineBottom.set(linePath)
        lineBottom.lineTo(clipTop.right, clipTop.bottom) // end bottom
        lineBottom.lineTo(clipTop.left, clipTop.bottom) // start bottom
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
        lineTop.lineTo(clipBottom.right, clipBottom.bottom) // end bottom
        lineTop.lineTo(clipBottom.left, clipBottom.bottom) // start bottom
        lineTop.close()
        canvas.withClip(clipBottom) {
            drawPath(linePath, redLinePaint)
            withClip(lineBottom) {
                redDrawable.colors = intArrayOf(redBackground, 0)
                redDrawable.bounds = clipBottom.toRect()
                redDrawable.draw(this)
            }
        }

        // horizontal line
        canvas.drawLine(spaceStart, middleY, drawEnd, middleY, dashPaint)

        if (!drawTouchUI && startPriceTextView.text.isNotEmpty()) {
            val textY = (middleY - startPriceTextView.measuredHeight / 2)
                .coerceAtMost(drawBottom - startPriceTextView.measuredHeight)
            val textX = spaceStart + startPriceMarginStart
            canvas.withTranslation(textX, textY) {
                startPriceTextView.draw(this)
            }
        }

        if (!drawTouchUI) {
            clearHapticFeedback()
            return
        }

        // vertial line
        val touchX = touchEvent.x
            .coerceAtLeast(spaceStart)
            .coerceAtMost(drawEnd)
        val deltaTouchX = touchX - spaceStart
        val percentTouchX = deltaTouchX / avaWidth
        val touchTime = minTime + percentTouchX * timeRange
        val touchIndex = findTouchTimeIndex(touchTime)
        val touchData = graphData[touchIndex]
        hapticFeedbackIfChanged(touchData[0])

        onDataTouchListener?.invoke(touchIndex)

        calPoint(
            outPoint = tmpPoint,
            data = touchData,
            minTime = minTime,
            timeRange = timeRange,
            spaceStart = spaceStart,
            avaWidth = avaWidth,
            minPrice = minPrice,
            priceRange = priceRange,
            spaceTop = spaceTop,
            avaHeight = avaHeight,
        )

        canvas.drawLine(tmpPoint.x, spaceTop, tmpPoint.x, drawBottom, dashPaint)

        // circle
        canvas.drawCircle(tmpPoint.x, tmpPoint.y, outterCircleRadius, outterCirclePaint)
        canvas.drawCircle(tmpPoint.x, tmpPoint.y, innerCircleRadius, innerCirclePaint)
    }

    private fun findTouchTimeIndex(touchTime: Double): Int {
        var low = 0
        var high = graphData.size - 1
        var minDelta = Double.MAX_VALUE
        var minIndex = 0

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows
            val midVal = graphData[mid]
            val time = midVal[0]
            val delta = abs(time - touchTime)
            if (delta < minDelta) {
                minIndex = mid
                minDelta = delta
            }

            val cmp = time.compareTo(touchTime)

            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid
            } // key found
        }
        return minIndex
    }

    private var drawTouchUI = false
    private lateinit var touchEvent: MotionEvent

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            drawTouchUI = false
            onDataTouchListener?.invoke(null)
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
