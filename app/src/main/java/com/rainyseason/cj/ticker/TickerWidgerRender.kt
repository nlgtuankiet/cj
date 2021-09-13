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
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.dpToPxF
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.setBackgroundResource
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

data class CoinTickerRenderParams(
    val config: CoinTickerConfig,
    val data: CoinTickerDisplayData,
    val showLoading: Boolean = false,
    val isPreview: Boolean = false,
    val userCurrency: String,
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
        params: CoinTickerRenderParams,
    ) {
        val config = params.config
        val renderData = params.data
        val backgroundRes = if ((renderData.getChangePercent(config) ?: 0.0) > 0) {
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
    fun RemoteViews.applyClickAction(params: CoinTickerRenderParams) {
        if (params.isPreview) {
            return
        }
        val config = params.config
        val layoutRes = appWidgetManager.getAppWidgetInfo(config.widgetId)?.initialLayout
            ?: R.layout.widget_coin_ticker_2x2_default
        val clazz = when (layoutRes) {
            R.layout.widget_coin_ticker_2x2_default -> CoinTickerProviderDefault::class.java
            R.layout.widget_coin_ticker_2x2_graph -> CoinTickerProviderGraph::class.java
            R.layout.widget_coin_ticker_2x2_coin360 -> CoinTickerProviderCoin360::class.java
            else -> error("Unknown layout for $layoutRes")
        }
        val pendingIntent = when (params.config.clickAction) {
            CoinTickerConfig.ClickAction.REFRESH -> {
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
            CoinTickerConfig.ClickAction.SWITCH_PRICE_MARKET_CAP -> {
                val intent = Intent(context, clazz)
                intent.action = CoinTickerConfig.Action.SWITCH_ACTION
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    params.config.widgetId,
                )
                PendingIntent.getBroadcast(
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
        params: CoinTickerRenderParams,
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

        val changePercent = renderData.getChangePercent(config)
        val changes = formatChange(params)
        view.setTextViewText(R.id.change_percent, changes)

        view.setViewVisibility(
            R.id.progress_bar,
            if (params.showLoading) View.VISIBLE else View.GONE
        )

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
            val data = renderData.getGraphData(config).orEmpty()
            val filteredData = data.filter { it.size == 2 && it[1] != 0.0 }
            if (filteredData.size >= 2) {
                val extraSize = config.extraSize
                val width = context.dpToPxF(110 + extraSize - 12 * 2f)
                val height = width / 2
                val isPositive = filteredData.last()[1] > filteredData.first()[1]
                val bitmap = createGraphBitmap(
                    context = context,
                    width = width,
                    height = height,
                    isPositive = isPositive || DebugFlag.POSITIVE_WIDGET.isEnable,
                    data = data
                )
                view.setImageViewBitmap(R.id.graph, bitmap)
            }
        }
    }

    fun render(
        view: RemoteViews,
        params: CoinTickerRenderParams,
    ) {
        val config = params.config

        var newParam = params
        if (DebugFlag.POSITIVE_WIDGET.isEnable) {
            newParam = params.copy(
                data = params.data.copy(
                    priceChangePercent = params.data.priceChangePercent?.let { abs(it) },
                    marketCapChangePercent = params.data.marketCapChangePercent?.let { abs(it) },
                )
            )
        }

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
            CoinTickerConfig.Layout.COIN360 -> renderCoin360(view, newParam)
            else -> renderDefault(view, newParam)
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

    /**
     * Ex: 2 decimal
     * ....**
     * 0.001234
     * -> 4
     */
    private fun getSmartNumberOfDecimal(amount: Double, configNumberOfDecimal: Int?): Int {
        if (configNumberOfDecimal == null) {
            return Int.MAX_VALUE
        }
        if (configNumberOfDecimal == 0) {
            return 0
        }
        if (amount == 0.0) {
            return 0
        }
        if (amount >= 100) {
            return 0
        }
        if (amount >= 0.1) {
            return configNumberOfDecimal
        }
        var result = 0
        var tempAmount = amount
        while (floor(tempAmount * 10).toInt() == 0) {
            result++
            tempAmount *= 10
        }
        result += configNumberOfDecimal
        return result
    }

    private fun formatChange(
        params: CoinTickerRenderParams,
        withColor: Boolean = true,
    ): CharSequence {
        val config = params.config
        val data = params.data

        @Suppress("UnnecessaryVariable")
        val content = buildSpannedString {
            val amount = data.getChangePercent(config)

            if (amount != null) {
                appendChange(
                    amount = amount,
                    numberOfDecimal = config.numberOfChangePercentDecimal,
                    withColor = withColor
                )
            }
        }
        return content
    }

    private fun formatAmount(
        params: CoinTickerRenderParams,
    ): String {
        val config = params.config
        val data = params.data
        var amount = data.getAmount(config)

        val roundToM = config.roundToMillion && amount > 1_000_000
        if (roundToM) {
            amount /= 1_000_000
        }
        val currencyCode = config.currency ?: params.userCurrency
        val currencyInfo = SUPPORTED_CURRENCY[currencyCode]
        if (currencyInfo == null) {
            error("Unknown $currencyInfo")
        }
        val locale = currencyInfo.locale
        val formatter: DecimalFormat = NumberFormat.getCurrencyInstance(locale) as DecimalFormat
        formatter.currency = Currency.getInstance(locale)
        if (!config.showCurrencySymbol) {
            val symbol = formatter.decimalFormatSymbols
            symbol.currencySymbol = ""
            formatter.decimalFormatSymbols = symbol
        }
        formatter.maximumFractionDigits = getSmartNumberOfDecimal(
            amount,
            config.numberOfAmountDecimal
        )
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