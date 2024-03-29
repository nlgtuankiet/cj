package com.rainyseason.cj.ticker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Looper
import android.util.Size
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.os.BuildCompat
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
import com.rainyseason.cj.common.WidgetRenderUtil
import com.rainyseason.cj.common.addFlagMutable
import com.rainyseason.cj.common.await
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.getAppWidgetSizes
import com.rainyseason.cj.common.getNonNullCurrencyInfo
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.model.Backend
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
import com.rainyseason.cj.tracking.EventName
import com.rainyseason.cj.tracking.EventParamKey
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
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
    private val notificationManager: NotificationManagerCompat,
) {
    private val notiChannelId = "ticker_widget_channel"

    private fun getSmallIcon(config: CoinTickerConfig): IconCompat {
        val res = when (config.backend) {
            Backend.CoinGecko -> when (config.coinId) {
                "bitcoin" -> R.drawable.notification_bitcoin
                "ethereum" -> R.drawable.notification_eth
                "binancecoin" -> R.drawable.notification_bnb
                "cardano" -> R.drawable.notification_ada
                "solana" -> R.drawable.notification_sol
                "ripple" -> R.drawable.notification_xrp
                "polkadot" -> R.drawable.notification_dot
                "terra-luna" -> R.drawable.notification_luna
                "dogecoin" -> R.drawable.notification_doge
                else -> R.drawable.notification_bitcoin
            }
            else -> R.drawable.notification_bitcoin
        }
        return IconCompat.createWithResource(context, res)
    }

    private fun postNotification(
        params: CoinTickerRenderParams
    ) {
        createNotificationChannel()
        val config = params.config
        val displayData = params.data
        val clickIntent = if (params.isPreview) {
            null
        } else {
            getClickIntent(params)
        }

        val smallView = RemoteViews(
            context.packageName,
            R.layout.widget_ticker_notification_small
        ).apply {
            val content = buildSpannedString {
                append(getDisplaySymbol(config, params.data))
                append(" • ")
                append(formatAmount(params))

                val color = renderUtil.getChangePercentColor(
                    theme = config.theme,
                    isPositiveOverride = getColorFactor(params)
                )
                color(color) {
                    append(" (")
                    append(formatChange(params))
                    append(")")
                }

                if (params.showLoading) {
                    append(" ⏳")
                }
            }
            setTextViewText(R.id.text, content)
            if (clickIntent != null) {
                setOnClickPendingIntent(R.id.content, clickIntent)
            }
        }

        val largeView = RemoteViews(
            context.packageName,
            R.layout.widget_ticker_notification_large
        ).apply {
            val content = buildSpannedString {
                append(getDisplaySymbol(config, params.data))
                val color = renderUtil.getChangePercentColor(
                    theme = config.theme,
                    isPositiveOverride = getColorFactor(params)
                )
                color(color) {
                    append(" (")
                    append(formatChange(params))
                    append(")")
                }

                if (params.showLoading) {
                    append(" ⏳")
                }
            }
            setTextViewText(R.id.text, content)

            val subtitle = buildString {
                append(formatAmount(params))
            }

            setTextViewText(R.id.subtitle, subtitle)

            val graph = graphRenderer.createGraphBitmap(
                context,
                theme = config.theme,
                inputWidth = context.dpToPx(120).toFloat(),
                inputHeight = context.dpToPx(40).toFloat(),
                data = getGraphData(config, displayData.priceGraph),
                isPositiveOverride = getColorFactor(params)
            )
            setImageViewBitmap(R.id.graph, graph)
            if (clickIntent != null) {
                setOnClickPendingIntent(R.id.content, clickIntent)
            }
        }

        val noti = NotificationCompat.Builder(context, notiChannelId)
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    setSmallIcon(getSmallIcon(config))
                }

                if (!BuildCompat.isAtLeastS()) {
                    setStyle(NotificationCompat.DecoratedCustomViewStyle())
                }
                // TODO find correct noti text color
                setCustomBigContentView(largeView)
                setCustomContentView(smallView)
                if (clickIntent != null) {
                    setContentIntent(clickIntent)
                }
                setOngoing(!params.isPreview)
                setSilent(true)
                setAutoCancel(false)
                setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            }
            .build()

        notificationManager.notify(getNotificationId(widgetId = config.widgetId), noti)
    }

    fun removeNotification(widgetId: Int) {
        notificationManager.cancel(getNotificationId(widgetId))
    }

    private fun getNotificationId(widgetId: Int): Int {
        return "ticker_widget_$widgetId".hashCode()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat.Builder(
            notiChannelId,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        ).apply {
            setName(context.getString(R.string.coin_ticker_notification_channel_name))
            setDescription(context.getString(R.string.coin_ticker_notification_channel_description))
        }.build()
        notificationManager.createNotificationChannel(channel)
    }

    private fun getClickIntent(params: CoinTickerRenderParams): PendingIntent {
        val config = params.config
        val componentName = appWidgetManager.getAppWidgetInfo(config.widgetId)?.provider
            ?: ComponentName(context, CoinTickerProviderDefault::class.java)
        return when (params.config.clickAction) {
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
                val intent = CoinTickerSettingActivity.starterIntent(context, config.widgetId)
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
                val intent = MainActivity.coinDetailIntent(context, config.getCoin())
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
                PendingIntent.getActivity(
                    context,
                    params.config.widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT.addFlagMutable()
                )
            }
            else -> error("Unknown action ${params.config.clickAction}")
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun RemoteViews.applyClickAction(params: CoinTickerRenderParams) {
        if (params.isPreview) {
            return
        }
        val pendingIntent = getClickIntent(params)
        setOnClickPendingIntent(R.id.content, pendingIntent)
    }

    private fun renderCoin360Mini(
        container: ViewGroup,
        params: CoinTickerRenderParams,
    ) {
        val binding = WidgetCoinTicker1x1Coin360MiniBinding
            .inflate(context.inflater(), container, true)
        val config = params.config

        container.mesureAndLayout(config)

        // bind container
        val backgroundRes = renderUtil.getBackgroundPositiveResource(
            theme = config.theme,
            isPositive = getColorFactor(params)
        )
        binding.container.setBackgroundResource(backgroundRes)
        applyBackgroundTransparency(binding.container, config)

        // bind symbol
        binding.symbol.text = getDisplaySymbol(config, params.data)

        // bind amount
        binding.amount.text = formatAmount(params)
    }

    /**
     * @return
     *  true mean positive
     *  false mean negative
     */
    private fun getColorFactor(params: CoinTickerRenderParams): Boolean {
        var changePercent = getChangePercent(params) ?: 0.0
        if (params.config.reversePositiveColor) {
            changePercent *= -1.0
        }
        return changePercent > 0
    }

    private fun getChangePercent(params: CoinTickerRenderParams): Double? {
        return if (params.config.reversePair) {
            // need to recalculate change percent
            params.data.priceChangePercent?.let { it * -1 }
        } else {
            params.data.priceChangePercent
        }
    }

    fun getDisplayName(
        config: CoinTickerConfig,
        data: CoinTickerDisplayData?,
    ): String {
        if (data == null) {
            return "⏳"
        }

        val displayName = config.displayName
        if (displayName != null) {
            return displayName
        }

        return if (config.backend.isExchange) {
            config.backend.displayName
        } else {
            data.name
        }
    }

    private val pairSeparatorRegex = """([-/])""".toRegex()

    fun getDisplaySymbol(
        config: CoinTickerConfig,
        data: CoinTickerDisplayData?,
    ): String {
        if (data == null) {
            return "⏳"
        }

        val displaySymbol = config.displaySymbol
        if (displaySymbol != null) {
            return displaySymbol
        }

        if (!config.reversePair) {
            return data.symbol.uppercase()
        }

        // need to reverse pair
        if (config.backend.isExchange) {
            // exchange pairs usually in the form of AAA/BBB or AAA-BBB
            // todo fix LUNO exchange is getting incorrect symbol
            val group = pairSeparatorRegex.find(data.symbol)?.groups?.get(1)
                ?: return "${data.symbol.uppercase()}\uD83E\uDD14?"
            val separatorIndex = group.range.first
            return buildString {
                append(data.symbol.substring(separatorIndex + 1))
                append(group.value)
                append(data.symbol.substring(0, separatorIndex))
            }.uppercase()
        }

        // case reverse pair and from price tracker
        return "${data.symbol}/${config.currency}".uppercase()
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
            isPositive = getColorFactor(params)
        )
        binding.container.setBackgroundResource(backgroundRes)
        applyBackgroundTransparency(binding.container, config)

        // bind symbol
        binding.symbol.text = getDisplaySymbol(config, params.data)

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
            getDisplayName(config, params.data)
        } else {
            getDisplaySymbol(config, params.data)
        }
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.symbol.updateVerticalFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.amount.updateVerticalFontMargin(updateBottom = true)

        // bind change percent
        binding.changePercent.text = formatChange(params)

        // bind name
        binding.name.text = if (config.backend.isExchange) {
            getDisplaySymbol(config, params.data)
        } else {
            getDisplayName(config, params.data)
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
        binding.symbol.text = getDisplaySymbol(config, params.data)
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))

        // bind change percent
        binding.changePercent.text = formatChange(params)

        binding.symbol.updateVerticalFontMargin(updateTop = true)
        binding.changePercent.updateVerticalFontMargin(updateBottom = true)
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
        binding.symbol.text = getDisplaySymbol(config, params.data)
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.symbol.updateVerticalFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))

        // bind change percent
        binding.changePercent.text = formatChange(params)
        binding.changePercent.updateVerticalFontMargin(updateBottom = true)

        drawGraph(container, binding.graph, params)
    }

    private fun getGraphData(
        config: CoinTickerConfig,
        data: List<List<Double>>?
    ): List<List<Double>> {
        if (data.isNullOrEmpty()) {
            return emptyList()
        }
        var graphData = data
        if (config.reversePair) {
            graphData = graphData
                .filter {
                    it.getOrNull(1).let { price -> price != null && price != 0.0 }
                }
                .map {
                    listOf(it[0], 1.0 / it[1])
                }
        }
        return graphData
    }

    private fun drawGraph(
        container: ViewGroup,
        imageView: ImageView,
        params: CoinTickerRenderParams,
    ) {
        val config = params.config
        val graphData = getGraphData(config, params.data.priceGraph)
        if (graphData.size >= 2) {
            container.mesureAndLayout(config)
            val width = imageView.measuredWidth.toFloat()
            val height = imageView.measuredHeight.toFloat()
            val bitmap = graphRenderer.createGraphBitmap(
                context = context,
                theme = params.config.theme,
                inputWidth = width,
                inputHeight = height,
                data = graphData,
                isPositiveOverride = getColorFactor(params)
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
        binding.symbol.text = getDisplaySymbol(config, params.data)
        binding.symbol.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.symbol.updateVerticalFontMargin(updateTop = true)

        // bind amount
        binding.amount.text = formatAmount(params)
        binding.amount.setTextColor(renderUtil.getTextPrimaryColor(theme))
        binding.amount.updateVerticalFontMargin(updateBottom = true)

        // bind change percent
        binding.changePercent.text = formatChange(params)
        binding.changePercent.updateVerticalFontMargin(updateTop = true)

        // bind name
        binding.name.text = getDisplayName(config, params.data)
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
            val keyValue = options.keySet().associateWith { options.get(it) }
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
            val width = when {
                minHeight == 0 -> {
                    // TODO log to crashlytic
                    height
                }
                minWidth / minHeight >= 2 -> {
                    height * 2
                }
                else -> {
                    height
                }
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

    private fun TextView.updateVerticalFontMargin(
        updateTop: Boolean = false,
        updateBottom: Boolean = false,
    ) {
        val verticalPadding = verticalPadding()
        updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(
                bottom = if (updateBottom) {
                    context.dpToPx(12) - verticalPadding.bottom
                } else {
                    bottomMargin
                },
                top = if (updateTop) {
                    context.dpToPx(12) - verticalPadding.top
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

    fun createPreviewRemoteView(
        layout: CoinTickerLayout,
        bitmap: Bitmap,
    ): RemoteViews {
        val view = RemoteViews(context.packageName, layout.layout)
        view.setImageViewBitmap(R.id.image_view, bitmap)
        view.setViewVisibility(R.id.progress_bar, View.GONE)
        return view
    }

    /**
     * @param inputRemoteView optional remote view to render (for preview)
     */
    suspend fun render(
        inputParams: CoinTickerRenderParams,
        inputRemoteView: RemoteViews? = null,
    ) {
        val config = inputParams.config
        val view = inputRemoteView ?: RemoteViews(context.packageName, config.layout.layout)
        view.bindLoading(inputParams)
        view.applyClickAction(inputParams)
        val icon: Bitmap? = if (config.layout.hasIcon) {
            GlideApp.with(context)
                .asBitmap()
                .override(context.dpToPx(48), context.dpToPx(48))
                .load(inputParams.data.iconUrl)
                .await(context)
        } else {
            null
        }

        if (inputParams.isPreview && DebugFlag.SHOW_PREVIEW_LAYOUT_BOUNDS.isEnable) {
            view as LocalRemoteViews
            val container = getContainer(inputParams, icon)
            view.container.removeAllViews()
            view.container.addView(container)
        } else {
            val bitmap = createBitmap(inputParams, icon)
            view.setImageViewBitmap(R.id.image_view, bitmap)
        }

        appWidgetManager.updateAppWidget(config.widgetId, view)
        if (config.showNotification) {
            postNotification(inputParams)
        } else {
            removeNotification(config.widgetId)
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

        return buildSpannedString {
            val amount = getChangePercent(params)

            if (amount != null) {
                val color = renderUtil.getChangePercentColor(
                    theme = config.theme,
                    isPositiveOverride = getColorFactor(params)
                )
                val locate = getNonNullCurrencyInfo(config.currency).locale
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
    }

    private fun formatAmount(
        params: CoinTickerRenderParams,
    ): String {
        val config = params.config
        val data = params.data
        var amount = data.price ?: return context.getString(R.string.coin_preview_only)
        if (config.reversePair && amount != 0.0) {
            amount = 1 / amount
        }
        amount *= (config.amount ?: 1.0)
        return numberFormater.formatAmount(
            amount = amount,
            roundToMillion = config.roundToMillion,
            currencyCode = config.currency,
            numberOfDecimal = config.numberOfAmountDecimal ?: 2,
            hideOnLargeAmount = config.hideDecimalOnLargePrice,
            showCurrencySymbol = config.shouldShowCurrencySymbol,
            showThousandsSeparator = config.showThousandsSeparator,
        )
    }
}
