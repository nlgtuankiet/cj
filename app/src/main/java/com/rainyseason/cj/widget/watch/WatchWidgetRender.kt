package com.rainyseason.cj.widget.watch

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.util.Size
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.updateLayoutParams
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.MainActivity
import com.rainyseason.cj.R
import com.rainyseason.cj.common.GraphRenderer
import com.rainyseason.cj.common.NumberFormater
import com.rainyseason.cj.common.SUPPORTED_CURRENCY
import com.rainyseason.cj.common.WidgetColorResolver
import com.rainyseason.cj.common.addFlagMutable
import com.rainyseason.cj.common.asColorStateList
import com.rainyseason.cj.common.dpToPx
import com.rainyseason.cj.common.dpToPxF
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.setBackgroundColor
import com.rainyseason.cj.common.setBackgroundResource
import com.rainyseason.cj.databinding.WidgetWatchBinding
import com.rainyseason.cj.databinding.WidgetWatchEntryBinding
import com.rainyseason.cj.databinding.WidgetWatchEntryDividerBinding
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.widget.watch.fullsize.WatchWidgetService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchWidgetRender @Inject constructor(
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val graphRenderer: GraphRenderer,
    private val numberFormater: NumberFormater,
    private val colorResolver: WidgetColorResolver,
) {

    private fun FrameLayout.measureAndLayout(config: WatchConfig) {
        val size = getWidgetSize(config)
        val specsWidth = View.MeasureSpec.makeMeasureSpec(size.width, View.MeasureSpec.EXACTLY)
        val specsHeight = View.MeasureSpec.makeMeasureSpec(size.height, View.MeasureSpec.EXACTLY)
        layoutParams = ViewGroup.MarginLayoutParams(size.width, size.height)
        measure(specsWidth, specsHeight)
        layout(0, 0, specsWidth, specsHeight)
    }

    fun getWidgetSize(config: WatchConfig): Size {
        val options = appWidgetManager.getAppWidgetOptions(config.widgetId)
        val minWidth = context
            .dpToPx(
                (options[AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH] as? Int)
                    ?: WatchConfig.MIN_WIDGET_WIDTH
            )
        val minHeight = context
            .dpToPx(
                (options[AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT] as? Int)
                    ?: WatchConfig.MIN_WIDGET_WIDTH
            )
        val size = minHeight.coerceAtMost(minWidth)
            .coerceAtLeast(context.dpToPx(WatchConfig.MIN_WIDGET_WIDTH))
            .plus(context.dpToPx(config.sizeAdjustment))

        val separatorCount = config.layout.entryLimit - 1
        val finalWidth = size + context.dpToPx(separatorCount * 1)
        val finalHeight = when (config.layout) {
            WatchWidgetLayout.Watch4x2 -> finalWidth / 2
            WatchWidgetLayout.Watch4x4 -> finalWidth
        }
        return Size(finalWidth, finalHeight)
    }

    private fun RemoteViews.bindLoading(params: WatchWidgetRenderParams) {
        setViewVisibility(
            R.id.progress_bar,
            if (params.showLoading) View.VISIBLE else View.INVISIBLE
        )
    }

    private fun createFullSizeEntryView(
        params: WatchWidgetRenderParams,
        container: ViewGroup,
        height: Int,
        data: WatchDisplayEntryContent?
    ): View {
        val config = params.config
        val binding = WidgetWatchEntryBinding.inflate(container.inflater, container, false)
        binding.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            this.height = height
        }
        binding.root.measure(
            View.MeasureSpec.makeMeasureSpec(container.width, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
        )
        binding.root.layout(0, 0, container.width, height)
        val theme = config.theme

        binding.symbol.text = data?.symbol
        binding.symbol.setTextColor(colorResolver.getTextPrimaryColor(theme))

        binding.name.text = data?.name
        binding.name.setTextColor(colorResolver.getTextSecondaryColor(theme))

        val graph = data?.graph
        if (graph != null && graph.size >= 2) {
            val bitmap = graphRenderer.createGraphBitmap(
                context,
                config.theme,
                binding.graph.width.toFloat(),
                binding.graph.height.toFloat(),
                graph,
            )
            binding.graph.setImageBitmap(bitmap)
        }

        val price = data?.price
        val priceContent = if (price != null) {
            numberFormater.formatAmount(
                price,
                currencyCode = params.config.currency,
                roundToMillion = config.roundToMillion,
                numberOfDecimal = config.numberOfAmountDecimal,
                hideOnLargeAmount = config.hideDecimalOnLargePrice,
                showCurrencySymbol = config.showCurrencySymbol,
                showThousandsSeparator = config.showThousandsSeparator
            )
        } else {
            ""
        }

        binding.price.text = priceContent
        binding.price.setTextColor(colorResolver.getTextPrimaryColor(theme))

        val changePercentContent = formatChangePercent(data?.changePercent, params.config)
        binding.changePercent.text = changePercentContent
        return binding.root
    }

    private fun formatChangePercent(
        amount: Double?,
        config: WatchConfig,
    ): CharSequence {
        return buildSpannedString {
            if (amount != null) {
                val color = colorResolver.getChangePercentColor(config.theme, amount)
                val locate = SUPPORTED_CURRENCY[config.currency]!!.locale
                val content = numberFormater.formatPercent(
                    amount = amount,
                    locate = locate,
                    numberOfDecimals = config.numberOfChangePercentDecimal
                )

                color(color) {
                    append(content)
                }
            } else {
                val color = colorResolver.getTextSecondaryColor(config.theme)
                color(color) {
                    append("--")
                }
            }
        }
    }

    private fun render4x2(
        container: FrameLayout,
        params: WatchWidgetRenderParams
    ) {
        val binding = WidgetWatchBinding
            .inflate(context.inflater(), container, true)
        val config = params.config
        val theme = config.theme
        val renderData = params.data

        // bind container
        binding.container.backgroundTintList = colorResolver.getBackgroundColor(theme)
            .asColorStateList()
        val widgetSize = getWidgetSize(params.config)

        run {
            // constraint widget size
            val specsWidth = View.MeasureSpec.makeMeasureSpec(
                widgetSize.width,
                View.MeasureSpec.EXACTLY
            )
            val specsHeight = View.MeasureSpec.makeMeasureSpec(
                widgetSize.height,
                View.MeasureSpec.EXACTLY
            )
            binding.root.apply {
                layoutParams = FrameLayout.LayoutParams(widgetSize.width, widgetSize.height)
                measure(specsWidth, specsHeight)
                layout(0, 0, specsWidth, specsHeight)
            }
        }

        val entryLimit = params.config.layout.entryLimit
        val widgetHeight = widgetSize.height.toDouble()
        val totalSeparatorHeight = context.dpToPxF((entryLimit - 1) * 1f)
        val height = ((widgetHeight - totalSeparatorHeight) / entryLimit).toInt()

        renderData.entries.forEachIndexed { index, watchDisplayEntry ->
            val view = createFullSizeEntryView(params, container, height, watchDisplayEntry.content)
            Timber.d("add iew with height $height")
            binding.listContainer.addView(view)

            if (index != renderData.entries.lastIndex) {
                val dividerView = WidgetWatchEntryDividerBinding.inflate(view.inflater)
                dividerView.divider.setBackgroundColor(colorResolver.getDividerColor(theme))
                binding.listContainer.addView(dividerView.root)
            }
        }
        container.measureAndLayout(config)

        applyBackgroundTransparency(binding.container, config)
    }

    private fun RemoteViews.applyClickAction(params: WatchWidgetRenderParams) {
        if (params.isPreview) {
            return
        }

        val config = params.config

        @SuppressLint("UnspecifiedImmutableFlag")
        val pendingIntent = when (config.clickAction) {
            WatchClickAction.Refresh -> {
                val intent = Intent()
                intent.component = ComponentName(context, config.layout.providerName)
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
            WatchClickAction.OpenWatchlist -> {
                val intent = MainActivity.watchListIntent(context)
                PendingIntent.getActivity(
                    context,
                    params.config.widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT.addFlagMutable()
                )
            }
        }
        setOnClickPendingIntent(R.id.widget_container, pendingIntent)
    }

    private fun applyBackgroundTransparency(
        background: View,
        config: WatchConfig,
    ) {
        background.background?.mutate()?.apply {
            alpha = ((100 - config.backgroundTransparency.toDouble()) / 100 * 255).toInt()
        }
    }

    private fun createContainer(inputParams: WatchWidgetRenderParams): View {
        val container = FrameLayout(context)
        container.measureAndLayout(inputParams.config)
        when (inputParams.config.layout) {
            WatchWidgetLayout.Watch4x2 -> render4x2(container, inputParams)
            WatchWidgetLayout.Watch4x4 -> render4x2(container, inputParams)
        }
        container.measureAndLayout(inputParams.config)

        return container
    }

    fun createBitmap(inputParams: WatchWidgetRenderParams): Bitmap {
        val container = createContainer(inputParams)
        val size = getWidgetSize(inputParams.config)
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        container.draw(canvas)
        return bitmap
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun createFullSizeContainerView(params: WatchWidgetRenderParams): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.widget_watch_full_size)
        val config = params.config
        val theme = params.config.theme
        remoteView.setBackgroundResource(
            R.id.container,
            colorResolver.getBackgroundResource(theme)
        )

        remoteView.setViewVisibility(
            R.id.progress_bar,
            if (params.showLoading) {
                View.VISIBLE
            } else {
                View.GONE
            }
        )
        val adapterIntent = Intent(context, WatchWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        remoteView.setRemoteAdapter(
            R.id.content,
            adapterIntent
        )

        val intent = Intent(context, MainActivity::class.java)
        val pendingClickTemplate = PendingIntent.getActivity(
            context,
            params.config.widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT.addFlagMutable()
        )

        remoteView.setPendingIntentTemplate(R.id.content, pendingClickTemplate)
        return remoteView
    }

    fun createEmptyView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_empty_view)
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun createFullSizeEntryView(
        entry: WatchDisplayEntry,
        config: WatchConfig,
    ): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.widget_watch_full_size_item)
        val theme = config.theme
        val data = entry.content

        remoteView.setTextViewText(R.id.symbol, data?.symbol ?: "")
        remoteView.setTextColor(R.id.symbol, colorResolver.getTextPrimaryColor(theme))

        remoteView.setTextViewText(R.id.name, data?.name ?: "")
        remoteView.setTextColor(R.id.name, colorResolver.getTextSecondaryColor(theme))

        val graph = data?.graph
        if (graph != null && graph.size >= 2) {
            val graphWidth = context.dpToPx(55)
            val graphHeight = context.dpToPx(20)
            val bitmap = graphRenderer.createGraphBitmap(
                context,
                config.theme,
                graphWidth.toFloat(),
                graphHeight.toFloat(),
                graph,
            )
            remoteView.setImageViewBitmap(R.id.graph, bitmap)
        }

        val price = data?.price
        val priceContent = if (price != null) {
            numberFormater.formatAmount(
                price,
                currencyCode = config.currency,
                roundToMillion = config.roundToMillion,
                numberOfDecimal = config.numberOfAmountDecimal,
                hideOnLargeAmount = config.hideDecimalOnLargePrice,
                showCurrencySymbol = config.showCurrencySymbol,
                showThousandsSeparator = config.showThousandsSeparator
            )
        } else {
            ""
        }

        remoteView.setTextViewText(R.id.price, priceContent)
        remoteView.setTextColor(R.id.price, colorResolver.getTextPrimaryColor(theme))
        val changePercentContent = formatChangePercent(data?.changePercent, config)
        remoteView.setTextViewText(R.id.change_percent, changePercentContent)

        val intent = MainActivity.coinDetailIntent(context, entry.coinId)
        remoteView.setOnClickFillInIntent(R.id.container, intent)

        return remoteView
    }

    fun createFullSizeSeparatorView(config: WatchConfig): RemoteViews {
        val remoteView = RemoteViews(context.packageName, R.layout.widget_watch_entry_divider)
        remoteView.setBackgroundColor(
            R.id.divider,
            colorResolver.getDividerColor(config.theme)
        )
        return remoteView
    }

    fun render(
        remoteView: RemoteViews,
        inputParams: WatchWidgetRenderParams
    ) {
        remoteView.bindLoading(inputParams)
        remoteView.applyClickAction(inputParams)

        if (inputParams.isPreview && DebugFlag.SHOW_PREVIEW_LAYOUT_BOUNDS.isEnable) {
            remoteView as LocalRemoteViews
            remoteView.container.removeAllViews()
            remoteView.container.addView(createContainer(inputParams))
        } else {
            val bitmap = createBitmap(inputParams)
            remoteView.setImageViewBitmap(R.id.image_view, bitmap)
        }
    }

    fun getWidgetRatio(config: WatchConfig): Size {
        return when (config.layout) {
            WatchWidgetLayout.Watch4x2 -> Size(4, 2)
            WatchWidgetLayout.Watch4x4 -> Size(4, 4)
        }
    }
}
