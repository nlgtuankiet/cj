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
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.withClip
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.rainyseason.cj.R
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.dpToPxF
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.verticalPadding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2Coin360Binding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2DefaultBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2GraphBinding
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
)

@Singleton
class TickerWidgetRenderer @Inject constructor(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
) {

    @LayoutRes
    fun selectLayout(config: CoinTickerConfig): Int {
        return R.layout.widget_square_2x2
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
        setOnClickPendingIntent(R.id.content, pendingIntent)
    }


    private fun renderCoin360(
        container: ViewGroup,
        remoteViews: RemoteViews,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x2Coin360Binding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind loading
        remoteViews.bindLoading(params)

        // bind container
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
        binding.container.setBackgroundResource(backgroundRes)

        // bind remote view
        remoteViews.applyClickAction(params)

        // bind symbol
        binding.symbol.text = renderData.symbol

        // bind change percent
        binding.changePercent.text = formatChange(params = params, withColor = false)

        // bind amount
        binding.amount.text = formatAmount(params)
    }

    private fun renderDefault(
        container: ViewGroup,
        remoteViews: RemoteViews,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x2DefaultBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        container.mesureAndLayout(config)

        remoteViews.bindLoading(params)

        // bind container
        binding.container.setBackgroundResource(
            select(
                theme,
                R.drawable.coin_ticker_background,
                R.drawable.coin_ticker_background_dark
            )
        )
        remoteViews.applyClickAction(params)

        // bind symbol
        binding.symbol.text = renderData.symbol
        binding.symbol.setTextColor(
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )
        binding.symbol.updateVertialFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )
        binding.amount.updateVertialFontMargin(updateBottom = true)

        // bind change percent
        binding.changePercent.text = formatChange(params)

        // bind name
        binding.name.text = renderData.name
        binding.name.setTextColor(
            select(
                theme,
                context.getColorCompat(R.color.gray_500),
                context.getColorCompat(R.color.gray_50),
            )
        )
    }

    private fun renderGraph(
        container: ViewGroup,
        remoteViews: RemoteViews,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x2GraphBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        container.mesureAndLayout(config)

        remoteViews.bindLoading(params)

        // bind container
        binding.container.setBackgroundResource(
            select(
                theme,
                R.drawable.coin_ticker_background,
                R.drawable.coin_ticker_background_dark
            )
        )
        remoteViews.applyClickAction(params)

        // bind symbol
        binding.symbol.text = renderData.symbol
        binding.symbol.setTextColor(
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )
        binding.symbol.updateVertialFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )
        binding.amount.updateVertialFontMargin(updateBottom = true)

        // bind change percent
        binding.changePercent.text = formatChange(params)
        binding.changePercent.updateVertialFontMargin(updateTop = true)

        // bind name
        binding.name.text = renderData.name
        binding.name.setTextColor(
            select(
                theme,
                context.getColorCompat(R.color.gray_500),
                context.getColorCompat(R.color.gray_50),
            )
        )

        val graphData = renderData.getGraphData(config).orEmpty()
        val filteredData = graphData.filter { it.size == 2 && it[1] != 0.0 }
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
                data = graphData
            )
            binding.graph.setImageBitmap(bitmap)
        }
    }

    private fun RemoteViews.bindLoading(params: CoinTickerRenderParams) {
        setViewVisibility(
            R.id.progress_bar,
            if (params.showLoading) View.VISIBLE else View.GONE
        )
    }

    private fun CoinTickerRenderParams.maybePositive(): CoinTickerRenderParams {
        if (DebugFlag.POSITIVE_WIDGET.isEnable) {
            return this.copy(
                data = this.data.copy(
                    priceChangePercent = this.data.priceChangePercent?.let { abs(it) },
                    marketCapChangePercent = this.data.marketCapChangePercent?.let { abs(it) },
                )
            )
        }
        return this
    }

    private fun ViewGroup.mesureAndLayout(config: CoinTickerConfig) {
        val size = getWidgetSize(config.widgetId)
        val specs = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        layoutParams = ViewGroup.MarginLayoutParams(size, size)
        measure(specs, specs)
        layout(0, 0, size, size)
    }

    fun getWidgetSize(widgetId: Int): Int {
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val minWidth = context
            .dpToPx((options[AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH] as? Int) ?: 155)
        val minHegth = context
            .dpToPx((options[AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT] as? Int) ?: 155)
        return minHegth.coerceAtMost(minWidth)
            .coerceAtMost(context.dpToPx(155))
            .coerceAtLeast(context.dpToPx(145))
    }

    private fun TextView.updateVertialFontMargin(
        updateTop: Boolean = false,
        updateBottom: Boolean = false,
    ) {
        val vertialPadding = verticalPadding()
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(
                bottom = if (updateBottom) {
                    context.dpToPx(12) - vertialPadding.bottom
                } else {
                    bottomMargin
                },
                top = if (updateTop) {
                    context.dpToPx(12) - vertialPadding.top
                } else {
                    topMargin
                }
            )
        }
    }

    fun render(
        view: RemoteViews,
        inputParams: CoinTickerRenderParams,
    ) {
        val container = FrameLayout(context)
        container.mesureAndLayout(inputParams.config)
        val params = inputParams.maybePositive()
        when (inputParams.config.layout) {
            CoinTickerConfig.Layout.DEFAULT -> renderDefault(container, view, params)
            CoinTickerConfig.Layout.GRAPH -> renderGraph(container, view, params)
            CoinTickerConfig.Layout.COIN360 -> renderCoin360(container, view, params)
            else -> error("Unknown layout: ${params.config.layout}")
        }

        container.mesureAndLayout(params.config)
        val size = getWidgetSize(params.config.widgetId)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        container.draw(canvas)
        view.setImageViewBitmap(R.id.image_view, bitmap)
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
        val currencyCode = config.currency
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