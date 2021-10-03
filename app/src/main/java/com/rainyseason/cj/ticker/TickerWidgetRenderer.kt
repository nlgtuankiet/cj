package com.rainyseason.cj.ticker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Size
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.R
import com.rainyseason.cj.common.GraphRenderer
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.verticalPadding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x1MiniBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2Coin360Binding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2DefaultBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2GraphBinding
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.tracking.Tracker
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs


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
    private val tracker: Tracker,
    private val numberFormater: NumberFormater,
    private val graphRenderer: GraphRenderer,
) {

    @LayoutRes
    fun selectLayout(config: CoinTickerConfig): Int {
        return when (config.layout) {
            CoinTickerConfig.Layout.GRAPH -> R.layout.widget_coin_ticker_2x2
            CoinTickerConfig.Layout.DEFAULT -> R.layout.widget_coin_ticker_2x2
            CoinTickerConfig.Layout.COIN360 -> R.layout.widget_coin_ticker_2x2
            CoinTickerConfig.Layout.MINI -> R.layout.widget_coin_ticker_2x1
            else -> error("not support ${config.layout}")
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
            R.layout.widget_coin_ticker_2x1_mini -> CoinTickerProviderMini::class.java
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
        applyBackgroundTransparency(binding.container, config)

        // bind remote view
        remoteViews.applyClickAction(params)

        // bind symbol
        binding.symbol.text = renderData.symbol

        // bind change percent
        binding.changePercent.text = formatChange(params = params, withColor = false)

        // bind amount
        binding.amount.text = formatAmount(params)
    }

    private fun applyBackgroundTransparency(
        background: View,
        config: CoinTickerConfig,
    ) {
        background.background?.mutate()?.apply {
            alpha = ((100 - config.backgroundTransparency.toDouble()) / 100 * 255).toInt()
        }
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
        applyBackgroundTransparency(binding.container, config)

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


        container.mesureAndLayout(config)

        run {
            // graph between change percent and amount is 12dp
            val currentGap = binding.run {
                changePercent.verticalPadding().bottom + amount.verticalPadding().top
            }
            binding.changePercent.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(bottom = -currentGap + context.dpToPx(12))
            }
        }

        run {
            // graph between symbol and name is 12dp
            val currentGap = binding.run {
                symbol.verticalPadding().bottom + name.verticalPadding().top
            }
            binding.name.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(top = -currentGap + context.dpToPx(12))
            }
        }

    }

    private fun renderMini(
        container: ViewGroup,
        remoteViews: RemoteViews,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x1MiniBinding
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
        applyBackgroundTransparency(binding.container, config)
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

        // bind change percent
        binding.changePercent.text = formatChange(params)
        binding.changePercent.updateVertialFontMargin(updateBottom = true)


        drawGraph(container, binding.graph, params)
    }

    private fun drawGraph(
        container: ViewGroup,
        imageView: ImageView,
        params: CoinTickerRenderParams,
    ) {
        val data = params.data
        val config = params.config
        val graphData = data.getGraphData(config).orEmpty()
        val filteredData = graphData.filter { it.size == 2 && it[1] != 0.0 }
        if (filteredData.size >= 2) {
            container.mesureAndLayout(config)
            val width = imageView.measuredWidth.toFloat()
            val height = imageView.measuredHeight.toFloat()
            val isPositive = filteredData.last()[1] > filteredData.first()[1]
            val renderData = if (!isPositive && DebugFlag.POSITIVE_WIDGET.isEnable) {
                filteredData.mapIndexed { index, point ->
                    listOf(point[0], filteredData[filteredData.size - 1 - index][1])
                }
            } else {
                filteredData
            }
            val bitmap = graphRenderer.createGraphBitmap(
                context = context,
                width = width,
                height = height,
                isPositive = isPositive || DebugFlag.POSITIVE_WIDGET.isEnable,
                data = renderData
            )
            imageView.setImageBitmap(bitmap)
        }
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
        applyBackgroundTransparency(binding.container, config)
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

        container.mesureAndLayout(config)

        run {
            // config gap between graph and amount to 12dp
            val amountTopGap = binding.amount.verticalPadding().top
            binding.graph.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(bottom = -amountTopGap + context.dpToPx(12))
            }
        }

        run {
            // gap between symbol and name is 8dp
            val currenGap = binding.run {
                symbol.verticalPadding().bottom + name.verticalPadding().top
            }
            binding.name.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(top = -currenGap + context.dpToPx(8))
            }
        }

        run {
            // gap between name and graph is 12dp
            val currenGap = binding.name.verticalPadding().bottom

            binding.graph.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(top = -currenGap + context.dpToPx(12))
            }
        }

        drawGraph(container, binding.graph, params)
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
        val size = getWidgetSize(config)
        val specsWidth = MeasureSpec.makeMeasureSpec(size.width, MeasureSpec.EXACTLY)
        val specsHeight = MeasureSpec.makeMeasureSpec(size.height, MeasureSpec.EXACTLY)
        layoutParams = ViewGroup.MarginLayoutParams(size.width, size.width)
        measure(specsWidth, specsHeight)
        layout(0, 0, specsWidth, specsHeight)
    }

    fun getWidgetSize(config: CoinTickerConfig): Size {
        val options = appWidgetManager.getAppWidgetOptions(config.widgetId)
        val minWidth = context
            .dpToPx((options[AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH] as? Int) ?: 155)
        val minHegth = context
            .dpToPx((options[AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT] as? Int) ?: 155)
        val size = minHegth.coerceAtMost(minWidth)
            .coerceAtMost(context.dpToPx(155))
            .coerceAtLeast(context.dpToPx(145))
        val finalWidth = size + context.dpToPx(config.sizeAdjustment)
        val finalHeight = when (config.layout) {
            CoinTickerConfig.Layout.MINI -> finalWidth / 2
            CoinTickerConfig.Layout.DEFAULT -> finalWidth
            CoinTickerConfig.Layout.GRAPH -> finalWidth
            CoinTickerConfig.Layout.COIN360 -> finalWidth
            else -> error("Unknown layout")
        }
        return Size(finalWidth, finalHeight)
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
        Timber.d("render showLoading: ${inputParams.showLoading}, isPreview: ${inputParams.isPreview}, config: ${inputParams.config}")
        val container = FrameLayout(context)
        container.mesureAndLayout(inputParams.config)
        val params = inputParams.maybePositive()
        when (inputParams.config.layout) {
            CoinTickerConfig.Layout.DEFAULT -> renderDefault(container, view, params)
            CoinTickerConfig.Layout.GRAPH -> renderGraph(container, view, params)
            CoinTickerConfig.Layout.COIN360 -> renderCoin360(container, view, params)
            CoinTickerConfig.Layout.MINI -> renderMini(container, view, params)
            else -> error("Unknown layout: ${params.config.layout}")
        }

        container.mesureAndLayout(params.config)
        val size = getWidgetSize(params.config)

        if (params.isPreview && DebugFlag.SHOW_PREVIEW_LAYOUT_BOUNDS.isEnable) {
            view as LocalRemoteViews
            view.container.removeAllViews()
            view.container.addView(container)
        } else {
            val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            container.draw(canvas)
            view.setImageViewBitmap(R.id.image_view, bitmap)
        }
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
                val color = if (amount > 0) {
                    ContextCompat.getColor(context, R.color.green_700)
                } else {
                    ContextCompat.getColor(context, R.color.red_600)
                }
                val locate = SUPPORTED_CURRENCY[config.currency]!!.locale
                val content = numberFormater.formatPercent(
                    amount = amount,
                    locate = locate,
                    numberOfDecimals = config.numberOfChangePercentDecimal ?: 1
                )
                if (withColor) {
                    color(color) {
                        append(content)
                    }
                } else {
                    append(content)
                }
            }
        }
        return content
    }

    private fun formatAmount(
        params: CoinTickerRenderParams,
    ): String {
        val config = params.config
        val data = params.data

        return numberFormater.formatAmount(
            amount = data.getAmount(config),
            roundToMillion = config.roundToMillion,
            currencyCode = config.currency,
            numberOfDecimal = config.numberOfAmountDecimal ?: 2,
            hideOnLargeAmount = config.hideDecimalOnLargePrice,
            showCurrencySymbol = config.showCurrencySymbol,
            showThousandsSeparator = config.showThousandsSeparator,
        )

    }
}