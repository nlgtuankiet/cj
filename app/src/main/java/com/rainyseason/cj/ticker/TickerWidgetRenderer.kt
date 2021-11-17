package com.rainyseason.cj.ticker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.MainActivity
import com.rainyseason.cj.R
import com.rainyseason.cj.common.GraphRenderer
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.verticalPadding
import com.rainyseason.cj.databinding.WidgetCoinTicker1x1Coin360MiniBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker1x1NanoBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x1MiniBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x1SmallIconBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2Coin360Binding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2DefaultBinding
import com.rainyseason.cj.databinding.WidgetCoinTicker2x2GraphBinding
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.tracking.Tracker
import javax.inject.Inject
import javax.inject.Singleton

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
        return CoinTickerConfig.Layout.getLayoutRes(config.layout)
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
        val componentName = appWidgetManager.getAppWidgetInfo(config.widgetId)?.provider
            ?: ComponentName(context, CoinTickerProviderDefault::class.java)

        val pendingIntent = when (params.config.clickAction) {
            CoinTickerConfig.ClickAction.REFRESH -> {
                val intent = Intent()
                intent.component = componentName
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
                val intent = Intent()
                intent.component = componentName
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
            CoinTickerConfig.ClickAction.OPEN_COIN_DETAIL -> {
                val intent = MainActivity.coinDetailIntent(context, config.coinId)
                PendingIntent.getActivity(
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

    private fun renderCoin360Mini(
        container: ViewGroup,
        remoteViews: RemoteViews,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker1x1Coin360MiniBinding
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

        // bind amount
        binding.amount.text = formatAmount(params)
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

    private fun renderNano(
        container: ViewGroup,
        remoteViews: RemoteViews,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker1x1NanoBinding
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

        binding.symbol.updateVertialFontMargin(updateTop = true)
        binding.changePercent.updateVertialFontMargin(updateBottom = true)
    }

    private fun renderIconSmall(
        container: ViewGroup,
        remoteViews: RemoteViews,
        params: CoinTickerRenderParams,
        icon: Bitmap?
    ) {
        val binding = WidgetCoinTicker2x1SmallIconBinding
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

        // bind icon
        val finalBitmap = icon ?: GlideApp.with(context)
            .asBitmap()
            .override(context.dpToPx(48), context.dpToPx(48))
            .load(renderData.iconUrl)
            .submit()
            .get()
        binding.icon.setImageBitmap(finalBitmap)
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
        if (graphData.size >= 2) {
            container.mesureAndLayout(config)
            val width = imageView.measuredWidth.toFloat()
            val height = imageView.measuredHeight.toFloat()
            val bitmap = graphRenderer.createGraphBitmap(
                context = context,
                width = width,
                height = height,
                data = graphData
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
        val minHeight = context
            .dpToPx((options[AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT] as? Int) ?: 155)
        val size = minHeight.coerceAtMost(minWidth)
            .coerceAtMost(context.dpToPx(155))
            .coerceAtLeast(context.dpToPx(145))
        val miniLayouts = listOf(
            CoinTickerConfig.Layout.COIN360_MINI,
            CoinTickerConfig.Layout.NANO,
            CoinTickerConfig.Layout.ICON_SMALL,
        )
        val finalSize = if (config.layout in miniLayouts) {
            val height = context.dpToPx(75)
            val width = if (minWidth / minHeight >= 2) {
                height * 2
            } else {
                height
            }
            Size(width, height)
        } else {
            val finalWidth = size + context.dpToPx(config.sizeAdjustment)
            val finalHeight = when (config.layout) {
                CoinTickerConfig.Layout.MINI -> finalWidth / 2
                CoinTickerConfig.Layout.DEFAULT -> finalWidth
                CoinTickerConfig.Layout.GRAPH -> finalWidth
                CoinTickerConfig.Layout.COIN360 -> finalWidth
                else -> error("Unknown layout")
            }
            Size(finalWidth, finalHeight)
        }

        return finalSize
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
        icon: Bitmap? = null,
    ) {
        val container = FrameLayout(context)
        container.mesureAndLayout(inputParams.config)
        when (inputParams.config.layout) {
            CoinTickerConfig.Layout.DEFAULT -> renderDefault(container, view, inputParams)
            CoinTickerConfig.Layout.GRAPH -> renderGraph(container, view, inputParams)
            CoinTickerConfig.Layout.COIN360 -> renderCoin360(container, view, inputParams)
            CoinTickerConfig.Layout.COIN360_MINI -> renderCoin360Mini(container, view, inputParams)
            CoinTickerConfig.Layout.MINI -> renderMini(container, view, inputParams)
            CoinTickerConfig.Layout.ICON_SMALL ->
                renderIconSmall(container, view, inputParams, icon)
            CoinTickerConfig.Layout.NANO -> renderNano(container, view, inputParams)
            else -> error("Unknown layout: ${inputParams.config.layout}")
        }

        container.mesureAndLayout(inputParams.config)
        val size = getWidgetSize(inputParams.config)

        if (inputParams.isPreview && DebugFlag.SHOW_PREVIEW_LAYOUT_BOUNDS.isEnable) {
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
