package com.rainyseason.cj.ticker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.withClip
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.rainyseason.cj.R
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.dpToPxF
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.setBackgroundResource
import com.rainyseason.cj.data.UserCurrency
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.roundToInt

data class TickerWidgetRenderParams(
    val userCurrency: UserCurrency,
    val config: CoinTickerConfig,
    val data: CoinTickerDisplayData,
    val showLoading: Boolean = false,
    val clickToUpdate: Boolean = false,
    val isPreview: Boolean = false,
)

@Singleton
class TickerWidgerRender @Inject constructor(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
) {

    @LayoutRes
    fun selectLayout(config: CoinTickerConfig): Int {
        return when (config.layout) {
            CoinTickerConfig.Layout.GRAPH -> R.layout.widget_coin_ticker_2x2_graph
            CoinTickerConfig.Layout.COIN360 -> R.layout.widget_coin_ticker_2x2_coin360
            else -> R.layout.widget_coin_ticker_2x2_default
        }
    }

    private fun <T> select(theme: String, light: T, dark: T): T {
        if (theme == Theme.LIGHT) {
            return light
        }
        if (theme == Theme.DARK) {
            return dark
        }
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (mode == Configuration.UI_MODE_NIGHT_YES) {
            return dark
        }
        return light
    }

    private fun renderCoin360(
        view: RemoteViews,
        params: TickerWidgetRenderParams,
    ) {
        val config = params.config
        val renderData = params.data
        val backgroundRes = if ((renderData.changePercent ?: 0.0) > 0) {
            select(
                config.theme,
                R.drawable.coin_ticker_background_positive_light,
                R.drawable.coin_ticker_background_positive_dark
            )
        } else {
            select(
                config.theme,
                R.drawable.coin_ticker_background_negative_light,
                R.drawable.coin_ticker_background_negative_dark
            )
        }
        view.setBackgroundResource(R.id.container, backgroundRes)
        view.applyClickAction(params)
        view.setTextViewText(R.id.symbol, renderData.symbol)

        val changes = formatChange(
            params = params,
            withColor = false
        )
        view.setTextViewText(R.id.change_percent, changes)

        val amountContent = formatAmount(params)
        view.setTextViewText(R.id.amount, amountContent)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun RemoteViews.applyClickAction(params: TickerWidgetRenderParams) {
        if (params.isPreview) {
            return
        }
        val config = params.config
        val pendingIntent = when (params.config.clickAction) {
            CoinTickerConfig.ClickAction.REFRESH -> {
                val layoutRes = appWidgetManager.getAppWidgetInfo(config.widgetId)?.initialLayout
                    ?: R.layout.widget_coin_ticker_2x2_default
                val clazz = when (layoutRes) {
                    R.layout.widget_coin_ticker_2x2_default -> CoinTickerProviderDefault::class.java
                    R.layout.widget_coin_ticker_2x2_graph -> CoinTickerProviderGraph::class.java
                    R.layout.widget_coin_ticker_2x2_coin360 -> CoinTickerProviderCoin360::class.java
                    else -> error("Unknown layout for $layoutRes")
                }
                val intent = Intent(context, clazz)
                intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    intArrayOf(params.config.widgetId)
                )
                PendingIntent.getBroadcast(
                    context,
                    params.config.widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            CoinTickerConfig.ClickAction.SETTING -> {
                val intent = Intent(context, CoinTickerSettingActivity::class.java)
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    params.config.widgetId,
                )
                intent.putExtra(
                    CoinTickerSettingActivity.COIN_ID_EXTRA,
                    params.config.coinId,
                )
                PendingIntent.getActivity(
                    context,
                    params.config.widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            else -> error("Unknown action ${params.config.clickAction}")
        }
        setOnClickPendingIntent(R.id.container, pendingIntent)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun renderDefault(
        view: RemoteViews,
        params: TickerWidgetRenderParams,
    ) {
        val config = params.config
        val theme = config.theme
        val renderData = params.data
        view.setBackgroundResource(
            R.id.container,
            select(
                theme,
                R.drawable.coin_ticker_background,
                R.drawable.coin_ticker_background_dark
            )
        )

        view.applyClickAction(params)

        view.setTextViewText(R.id.symbol, renderData.symbol)
        view.setTextColor(
            R.id.symbol,
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )

        val amountContent = formatAmount(params)
        view.setTextViewText(R.id.amount, amountContent)
        view.setTextColor(
            R.id.amount,
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )

        val changePercent = renderData.changePercent
        val changes = formatChange(params)
        view.setTextViewText(R.id.change_percent, changes)

        view.setViewVisibility(
            R.id.progress_bar,
            if (params.showLoading) View.VISIBLE else View.GONE
        )
        view.setImageViewBitmap(R.id.icon, renderData.iconBitmap)

        view.setTextViewText(R.id.name, renderData.name)
        view.setTextColor(
            R.id.name,
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )

        if (config.layout == CoinTickerConfig.Layout.GRAPH) {
            val data = renderData.graphData
            if (data != null && changePercent != null) {
                val extraSize = config.extraSize
                val width = context.dpToPxF(110 + extraSize - 12 * 2f)
                val height = width / 2
                val bitmap = createGraphBitmap(
                    context = context,
                    width = width,
                    height = height,
                    isPositive = changePercent > 0,
                    data = data
                )
                view.setImageViewBitmap(R.id.graph, bitmap)
            }
        }
    }

    fun render(
        view: RemoteViews,
        params: TickerWidgetRenderParams,
    ) {
        val config = params.config

        val visibleIndies = when (config.extraSize) {
            10 -> listOf(0)
            20 -> listOf(0, 1)
            30 -> listOf(0, 1, 2)
            else -> listOf()
        }
        visibleIndies.forEach { visibleIndex ->
            view.setViewVisibility(
                listOf(R.id.right_1, R.id.right_2, R.id.right_3)[visibleIndex],
                View.VISIBLE
            )
            view.setViewVisibility(
                listOf(R.id.bottom_1, R.id.bottom_2, R.id.bottom_3)[visibleIndex],
                View.VISIBLE
            )
        }

        when (config.layout) {
            CoinTickerConfig.Layout.COIN360 -> renderCoin360(view, params)
            else -> renderDefault(view, params)
        }
    }

    private fun SpannableStringBuilder.appendChange(
        amount: Double,
        numberOfDecimal: Int?,
        withColor: Boolean,
    ) {
        val color = if (amount > 0) {
            ContextCompat.getColor(context, R.color.green_700)
        } else {
            ContextCompat.getColor(context, R.color.red_600)
        }

        val amountText = if (numberOfDecimal != null) {
            "%.${numberOfDecimal}f".format(amount)
        } else {
            amount.toString()
        }
        val symbol = if (amount > 0) {
            "+"
        } else {
            ""
        }
        if (withColor) {
            color(color) {
                append("${symbol}${amountText}%")
            }
        } else {
            append("${symbol}${amountText}%")
        }

    }

    private fun formatChange(
        params: TickerWidgetRenderParams,
        withColor: Boolean = true,
    ): CharSequence {
        val config = params.config
        val data = params.data

        @Suppress("UnnecessaryVariable")
        val content = buildSpannedString {
            val amount = data.changePercent
            if (amount != null) {
                appendChange(
                    amount = amount,
                    numberOfDecimal = config.numberOfChangePercentDecimal,
                    withColor = withColor
                )
            } else {
                append(context.getString(R.string.loading))
            }
        }
        return content
    }

    private fun formatAmount(
        params: TickerWidgetRenderParams,
    ): String {
        val config = params.config
        val data = params.data
        var amount = data.amount
        val roundToM = amount >= 1_000_000
        if (roundToM) {
            amount = (amount / 1_000_000.0).roundToInt().toDouble()
        }

        val formatter: DecimalFormat = NumberFormat.getCurrencyInstance(Locale.US) as DecimalFormat
        formatter.currency = Currency.getInstance(Locale.US)
        formatter.maximumFractionDigits = config.numberOfPriceDecimal ?: Int.MAX_VALUE
        formatter.minimumFractionDigits = 0
        formatter.isGroupingUsed = config.showThousandsSeparator

        var formattedPrice = formatter.format(amount)
        if (roundToM) {
            formattedPrice += "M"
        }
        return formattedPrice
    }

    private fun createGraphBitmap(
        context: Context,
        width: Float,
        height: Float,
        isPositive: Boolean,
        data: List<List<Double>>,
    ): Bitmap {
        Timber.d("createGraphBitmap $width $height ${data.size}")
        // [0] timestamp
        // [1] price
        val bitmap = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
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

        val gradientColor = if (isPositive) {
            context.getColorCompat(R.color.ticket_line_green_background)
        } else {
            context.getColorCompat(R.color.ticket_line_red_background)
        }

        canvas.withClip(path) {
            val gradient = LinearGradient(
                width / 2f, minY, width / 2f, height,
                gradientColor,
                context.getColorCompat(android.R.color.transparent),
                Shader.TileMode.CLAMP
            )
            val gPaint = Paint()
            gPaint.isDither = true
            gPaint.shader = gradient
            drawRect(0f, 0f, width, height, gPaint)
        }

        return bitmap
    }
}