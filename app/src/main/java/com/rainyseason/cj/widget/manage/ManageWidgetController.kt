package com.rainyseason.cj.widget.manage

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.withState
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.R
import com.rainyseason.cj.common.BuildState
import com.rainyseason.cj.common.addFlagMutable
import com.rainyseason.cj.common.buildModels
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.isRequestPinAppWidgetSupportedCompat
import com.rainyseason.cj.common.view.textView
import com.rainyseason.cj.common.view.verticalSpacerView
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerLayout
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.CoinTickerSettingActivity
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logClick
import com.rainyseason.cj.widget.manage.view.WidgetParam
import com.rainyseason.cj.widget.manage.view.WidgetPreviewView
import com.rainyseason.cj.widget.manage.view.widgetPreviewContainer
import com.rainyseason.cj.widget.manage.view.widgetPreviewView
import com.rainyseason.cj.widget.manage.view.widgetView
import com.rainyseason.cj.widget.watch.WatchSettingActivity
import com.rainyseason.cj.widget.watch.WatchWidgetRender
import com.rainyseason.cj.widget.watch.WatchWidgetRenderParams
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ManageWidgetController @AssistedInject constructor(
    @Assisted private val viewModel: ManageWidgetViewModel,
    private val tickerWidgetRenderer: TickerWidgetRenderer,
    private val watchWidgetRenderer: WatchWidgetRender,
    private val context: Context,
    private val appWidgetManager: AppWidgetManager,
    private val tracker: Tracker,
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        if (!state.loadWidgetsDone) {
            return
        }

        verticalSpacerView {
            id("space_top")
            height(6)
        }

        val allWidgetIds = state.run { tickerConfigs.keys + watchConfig.keys }
            .distinct()
            .sortedDescending()

        allWidgetIds.forEach { widgetId ->
            val tickerConfig = state.tickerConfigs[widgetId]
            if (tickerConfig != null) {
                renderTickerWidget(state, widgetId)
            }

            val watchConfig = state.watchConfig[widgetId]
            if (watchConfig != null) {
                renderWatchWidget(state, widgetId)
            }
        }

        if (allWidgetIds.isEmpty()) {
            textView {
                id("empty_text")
                content(R.string.manage_widgets_empty)
                alignment(TextView.TEXT_ALIGNMENT_CENTER)
                paddingVertical(48)
                textColor(context.getColorCompat(R.color.text_secondary))
            }

            maybeBuildLegacyHowToAddWidget(state)
        }

        maybeBuildWidgetGallery(state)

        verticalSpacerView {
            id("space_bottom")
            height(12)
        }
    }

    private fun maybeBuildLegacyHowToAddWidget(state: ManageWidgetState): BuildState {
        if (appWidgetManager.isRequestPinAppWidgetSupportedCompat()) {
            return BuildState.Next
        }

        textView {
            id("tutorial_text")
            content(R.string.manage_widgets_how_to_add_widget)
            alignment(TextView.TEXT_ALIGNMENT_TEXT_START)
            paddingHorizontal(16)
            textColor(context.getColorCompat(R.color.text_secondary))
        }

        return BuildState.Next
    }

    private fun maybeBuildWidgetGallery(state: ManageWidgetState): BuildState {
        if (!appWidgetManager.isRequestPinAppWidgetSupportedCompat()) {
            return BuildState.Next
        }

        verticalSpacerView {
            id("widget_gallery_title_space_top")
            height(24)
        }

        textView {
            id("add_widget_shortcut_title")
            textSizeSp(20f)
            alignment(TextView.TEXT_ALIGNMENT_CENTER)
            content("Create widget shortcut")
            textColor(context.getColorCompat(R.color.text_primary))
            paddingHorizontal(12)
        }

        textView {
            id("add_widget_shortcut_subtitle")
            textSizeSp(14f)
            alignment(TextView.TEXT_ALIGNMENT_CENTER)
            content("Click on a widget preview below to add")
            textColor(context.getColorCompat(R.color.text_secondary))
            paddingHorizontal(12)
        }

        buildTickerPreviews(
            state,
            "2x2",
            CoinTickerLayout.ALL_2X2_LAYOUT
        )
        buildTickerPreviews(
            state,
            "2x1",
            CoinTickerLayout.ALL_2X1_LAYOUT
        )
        buildTickerPreviews(
            state,
            "1x1",
            CoinTickerLayout.ALL_1X1_LAYOUT
        )

        return BuildState.Next
    }

    private fun buildTickerPreviews(
        state: ManageWidgetState,
        group: String,
        layouts: List<CoinTickerLayout>
    ) {
        val displayDataAsync = state.previewCoinTickerDisplayData
        val displayData = displayDataAsync.invoke()

        verticalSpacerView {
            id("preview_separator_$group")
            height(12)
        }

        val models = buildModels {
            layouts.forEach { layout ->
                val config = CoinTickerConfig.DEFAULT_FOR_PREVIEW.copy(layout = layout)
                val params = WidgetPreviewView.Param(
                    ratio = layout.ratio,
                    renderParam = displayData?.let {
                        CoinTickerRenderParams(
                            config = config,
                            data = it
                        )
                    }
                )
                widgetPreviewView {
                    id("preview_${layout.id}")
                    isLoading(displayDataAsync is Loading)
                    renderParam(params)
                    onClickListener { view ->
                        this@ManageWidgetController.onTickerWidgetClick(layout, view)
                    }
                }
            }
        }
        widgetPreviewContainer {
            id("preview_$group")
            models(models)
        }
    }

    private fun onTickerWidgetClick(layout: CoinTickerLayout, view: View) {
        if (!appWidgetManager.isRequestPinAppWidgetSupportedCompat()) {
            Toast.makeText(context, "Not supported!", Toast.LENGTH_SHORT).show()
            FirebaseCrashlytics.getInstance().recordException(
                Exception("Pin app widget not supported")
            )
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        tracker.logClick(
            screenName = ManageWidgetFragment.SCREEN_NAME,
            target = "create_widget_shortcut",
            params = mapOf("layout" to layout.id)
        )

        val intent = Intent(context, RequestPinTickerWidgetReceiver::class.java)
        val componentName = ComponentName(context, layout.providerName)
        val bitmap = (view as? WidgetPreviewView)?.getPreviewBitmap()
        val extra = if (bitmap == null) {
            null
        } else {
            val remoteView = tickerWidgetRenderer.createPreviewRemoteView(layout, bitmap)
            bundleOf(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW to remoteView)
        }
        appWidgetManager.requestPinAppWidget(
            componentName,
            extra,
            PendingIntent.getBroadcast(
                context,
                layout.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT.addFlagMutable()
            )
        )
    }

    private fun renderWatchWidget(state: ManageWidgetState, widgetId: Int) {
        val config = state.watchConfig[widgetId]?.invoke() ?: return
        val displayData = state.watchDisplayData[widgetId]?.invoke() ?: return
        val refreshing = state.widgetLoading[widgetId]?.invoke() == true
        val params = WatchWidgetRenderParams(
            config = config,
            data = displayData,
            showLoading = false,
            isPreview = false
        )
        val ratio = watchWidgetRenderer.getWidgetRatio(config)
        widgetView {
            id(config.widgetId)
            title("#$widgetId")
            subtitle(config.layout.nameRes)
            widgetParams(
                WidgetParam(
                    ratio,
                    params
                )
            )
            isRefreshing(refreshing)
            onRefreshClickListener { _ ->
                viewModel.refreshWatchWidget(widgetId)
            }
            onClickListener { model, _, clickedView, _ ->
                val context = clickedView.context
                val currentParams = model.widgetParams().widgetRenderParam
                    as? WatchWidgetRenderParams ?: return@onClickListener
                context.startActivity(
                    WatchSettingActivity.starterIntent(context, currentParams.config)
                )
            }
        }
    }

    private fun renderTickerWidget(state: ManageWidgetState, widgetId: Int) {
        val config = state.tickerConfigs[widgetId]?.invoke() ?: return
        val displayData = state.tickerDisplayData[widgetId]?.invoke() ?: return
        val refreshing = state.widgetLoading[widgetId]?.invoke() == true
        val params = CoinTickerRenderParams(
            config = config,
            data = displayData,
            showLoading = false,
            isPreview = false
        )
        val ratio = config.layout.ratio
        val layoutNameRes = config.layout.titleRes
        widgetView {
            id(config.widgetId)
            title("#$widgetId")
            subtitle(layoutNameRes)
            widgetParams(
                WidgetParam(
                    ratio,
                    params
                )
            )
            isRefreshing(refreshing)
            onRefreshClickListener { _ ->
                viewModel.refreshTickerWidget(widgetId)
            }
            onClickListener { model, _, clickedView, _ ->
                val context = clickedView.context
                val currentParams = model.widgetParams().widgetRenderParam
                    as? CoinTickerRenderParams ?: return@onClickListener
                context.startActivity(
                    CoinTickerSettingActivity.starterIntent(context, currentParams.config.widgetId)
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(viewModel: ManageWidgetViewModel): ManageWidgetController
    }
}
