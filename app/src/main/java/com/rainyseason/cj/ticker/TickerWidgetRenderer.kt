package com.rainyseason.cj.ticker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.util.Size
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.MainActivity
import com.rainyseason.cj.R
import com.rainyseason.cj.common.GraphRenderer
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.WidgetRenderUtil
import com.rainyseason.cj.common.addFlagMutable
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.getAppWidgetSizes
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.model.WidgetRenderParams
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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class CoinTickerRenderParams(
    val config: CoinTickerConfig,
    val data: CoinTickerDisplayData,
    val showLoading: Boolean = false,
    val isPreview: Boolean = false,
) : WidgetRenderParams

@Singleton
class TickerWidgetRenderer @Inject constructor(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val tracker: Tracker,
    private val numberFormater: NumberFormater,
    private val graphRenderer: GraphRenderer,
    private val renderUtil: WidgetRenderUtil,
) {

    @LayoutRes
    fun selectLayout(config: CoinTickerConfig): Int {
        return config.layout.layout
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
                    PendingIntent.FLAG_UPDATE_CURRENT.addFlagMutable()
                )
            }
            CoinTickerConfig.ClickAction.SETTING -> {
                val intent = CoinTickerSettingActivity.starterIntent(context, config)
                PendingIntent.getActivity(
                    context,
                    params.config.widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT.addFlagMutable()
                )
            }
            // migrate switch action to open coin detail
            CoinTickerConfig.ClickAction.OPEN_COIN_DETAIL,
            CoinTickerConfig.ClickAction.SWITCH_PRICE_MARKET_CAP -> {
                val intent = MainActivity.coinDetailIntent(context, config.coinId)
                PendingIntent.getActivity(
                    context,
                    params.config.widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT.addFlagMutable()
                )
            }
            else -> error("Unknown action ${params.config.clickAction}")
        }
        setOnClickPendingIntent(R.id.content, pendingIntent)
    }

    private fun renderCoin360Mini(
        container: ViewGroup,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker1x1Coin360MiniBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind container
        val backgroundRes = renderUtil.getBackgroundPositiveResource(
            theme = config.theme,
            isPositive = (renderData.priceChangePercent ?: 0.0) > 0
        )
        binding.container.setBackgroundResource(backgroundRes)
        applyBackgroundTransparency(binding.container, config)

        // bind symbol
        binding.symbol.text = renderData.symbol

        // bind amount
        binding.amount.text = formatAmount(params)
    }

    private fun renderCoin360(
        container: ViewGroup,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x2Coin360Binding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind container
        val backgroundRes = renderUtil.getBackgroundPositiveResource(
            theme = config.theme,
            isPositive = (renderData.priceChangePercent ?: 0.0) > 0
        )
        binding.container.setBackgroundResource(backgroundRes)
        applyBackgroundTransparency(binding.container, config)

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
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x2DefaultBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind container
        binding.container.setBackgroundResource(renderUtil.getBackgroundResource(theme))
        applyBackgroundTransparency(binding.container, config)

        // bind symbol
        binding.symbol.text = if (config.backend.isExchange) {
            renderData.name
        } else {
            renderData.symbol.uppercase()
        }
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.symbol.updateVertialFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.amount.updateVertialFontMargin(updateBottom = true)

        // bind change percent
        binding.changePercent.text = formatChange(params)

        // bind name
        binding.name.text = if (config.backend.isExchange) {
            renderData.symbol
        } else {
            renderData.name
        }
        binding.name.setTextColor(renderUtil.getTextSecondaryColor(theme))

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
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker1x1NanoBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind container
        binding.container.setBackgroundResource(renderUtil.getBackgroundResource(theme))
        applyBackgroundTransparency(binding.container, config)

        // bind symbol
        binding.symbol.text = renderData.symbol
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))

        // bind change percent
        binding.changePercent.text = formatChange(params)

        binding.symbol.updateVertialFontMargin(updateTop = true)
        binding.changePercent.updateVertialFontMargin(updateBottom = true)
    }

    private fun renderIconSmall(
        container: ViewGroup,
        params: CoinTickerRenderParams,
        icon: Bitmap?
    ) {
        val binding = WidgetCoinTicker2x1SmallIconBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind container
        binding.container.setBackgroundResource(renderUtil.getBackgroundResource(theme))
        applyBackgroundTransparency(binding.container, config)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))

        // bind change percent
        binding.changePercent.text = formatChange(params)

        // bind icon
        val finalBitmap = icon ?: if (renderData.iconUrl.isNotBlank()) {
            // we will refresh the widget again after initial save
            if (Looper.myLooper() !== Looper.getMainLooper()) {
                GlideApp.with(context)
                    .asBitmap()
                    .override(context.dpToPx(48), context.dpToPx(48))
                    .load(renderData.iconUrl)
                    .submit()
                    .get()
            } else {
                null
            }
        } else {
            null
        }
        if (finalBitmap != null) {
            binding.icon.setImageBitmap(finalBitmap)
        }
    }

    private fun renderMini(
        container: ViewGroup,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x1MiniBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind container
        binding.container.setBackgroundResource(renderUtil.getBackgroundResource(theme))
        applyBackgroundTransparency(binding.container, config)

        // bind symbol
        binding.symbol.text = renderData.symbol
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.symbol.updateVertialFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))

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
        val graphData = data.priceGraph.orEmpty()
        if (graphData.size >= 2) {
            container.mesureAndLayout(config)
            val width = imageView.measuredWidth.toFloat()
            val height = imageView.measuredHeight.toFloat()
            val bitmap = graphRenderer.createGraphBitmap(
                context = context,
                theme = params.config.theme,
                inputWidth = width,
                inputHeight = height,
                data = graphData
            )
            imageView.setImageBitmap(bitmap)
        }
    }

    private fun renderGraph(
        container: ViewGroup,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker2x2GraphBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        container.mesureAndLayout(config)

        // bind container
        binding.container.setBackgroundResource(renderUtil.getBackgroundResource(theme))
        applyBackgroundTransparency(binding.container, config)

        // bind symbol
        binding.symbol.text = renderData.symbol
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.symbol.updateVertialFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.amount.updateVertialFontMargin(updateBottom = true)

        // bind change percent
        binding.changePercent.text = formatChange(params)
        binding.changePercent.updateVertialFontMargin(updateTop = true)

        // bind name
        binding.name.text = renderData.name
        binding.name.setTextColor(renderUtil.getTextSecondaryColor(theme))

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

    /**
     * TODO compat widget size follow ratio spec
     */
    fun getWidgetSize(config: CoinTickerConfig): Size {
        val options = appWidgetManager.getAppWidgetOptions(config.widgetId)
        if (BuildConfig.DEBUG) {
            val keyValue = options.keySet().map { it to options.get(it) }.toMap()
            Timber.d("options: $keyValue}")
        }

        if (config.fullSize) {
            val size = options.getAppWidgetSizes()?.firstOrNull()
            if (size != null && size.width > 0 && size.height > 0) {
                return Size(
                    context.dpToPx(size.width),
                    context.dpToPx(size.height),
                )
            }

            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            if (height > 0 && width > 0) {
                return Size(
                    context.dpToPx(width),
                    context.dpToPx(height),
                )
            }
        }

        val minWidth = context
            .dpToPx((options[AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH] as? Int) ?: 155)
        val minHeight = context
            .dpToPx((options[AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT] as? Int) ?: 155)
        val size = minHeight.coerceAtMost(minWidth)
            .coerceAtMost(context.dpToPx(155))
            .coerceAtLeast(context.dpToPx(145))
        val finalSize = if (config.layout.isNano) {
            val height = context.dpToPx(75) + context.dpToPx(config.sizeAdjustment)
            val width = if (minWidth / minHeight >= 2) {
                height * 2
            } else {
                height
            }
            Size(width, height)
        } else {
            val finalWidth = size + context.dpToPx(config.sizeAdjustment)
            val finalHeight = when (config.layout) {
                CoinTickerLayout.Graph2x1 -> finalWidth / 2
                CoinTickerLayout.Icon2x1 -> finalWidth / 2
                CoinTickerLayout.Default2x2 -> finalWidth
                CoinTickerLayout.Graph2x2 -> finalWidth
                CoinTickerLayout.Coin3602x2 -> finalWidth
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

    fun getContainer(
        inputParams: CoinTickerRenderParams,
        icon: Bitmap? = null
    ): View {
        val container = FrameLayout(context)
        container.mesureAndLayout(inputParams.config)
        when (inputParams.config.layout) {
            CoinTickerLayout.Default2x2 -> renderDefault(container, inputParams)
            CoinTickerLayout.Graph2x2 -> renderGraph(container, inputParams)
            CoinTickerLayout.Coin3602x2 -> renderCoin360(container, inputParams)
            CoinTickerLayout.Coin3601x1 -> renderCoin360Mini(container, inputParams)
            CoinTickerLayout.Graph2x1 -> renderMini(container, inputParams)
            CoinTickerLayout.Icon2x1 ->
                renderIconSmall(container, inputParams, icon)
            CoinTickerLayout.Nano1x1 -> renderNano(container, inputParams)
        }

        container.mesureAndLayout(inputParams.config)
        return container
    }

    fun render(
        view: RemoteViews,
        inputParams: CoinTickerRenderParams,
        icon: Bitmap? = null,
    ) {
        view.bindLoading(inputParams)
        view.applyClickAction(inputParams)

        if (inputParams.isPreview && DebugFlag.SHOW_PREVIEW_LAYOUT_BOUNDS.isEnable) {
            view as LocalRemoteViews
            val container = getContainer(inputParams, icon)
            view.container.removeAllViews()
            view.container.addView(container)
        } else {
            val bitmap = createBitmap(inputParams, icon)
            view.setImageViewBitmap(R.id.image_view, bitmap)
        }
    }

    fun createBitmap(inputParams: CoinTickerRenderParams, icon: Bitmap?): Bitmap {
        val container = getContainer(inputParams, icon)
        val size = getWidgetSize(inputParams.config)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        container.draw(canvas)
        return bitmap
    }

    private fun formatChange(
        params: CoinTickerRenderParams,
        withColor: Boolean = true,
    ): CharSequence {
        val config = params.config
        val data = params.data

        @Suppress("UnnecessaryVariable")
        val content = buildSpannedString {
            val amount = data.priceChangePercent

            if (amount != null) {
                val color = renderUtil.getChangePercentColor(config.theme, amount)
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
            } else {
                val color = renderUtil.getTextPrimaryColor(config.theme)
                color(color) {
                    append("--")
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
        val amount = data.getAmount(config)
            ?: return context.getString(R.string.coin_preview_only)
        return numberFormater.formatAmount(
            amount = amount,
            roundToMillion = config.roundToMillion,
            currencyCode = config.currency,
            numberOfDecimal = config.numberOfAmountDecimal ?: 2,
            hideOnLargeAmount = config.hideDecimalOnLargePrice,
            showCurrencySymbol = config.showCurrencySymbol,
            showThousandsSeparator = config.showThousandsSeparator,
        )
    }
}
