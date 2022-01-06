package com.rainyseason.cj.widget.manage

import android.content.Context
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.common.view.centerText
import com.rainyseason.cj.common.view.verticalSpacerView
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.CoinTickerSettingActivity
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.widget.manage.view.WidgetParam
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
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
        if (!state.loadWidgetsDone) {
            return
        }
        val allWidgetIds = state.run { tickerConfigs.keys + watchConfig.keys }
            .distinct()
            .sortedDescending()

        verticalSpacerView {
            id("space_top")
            height(6)
        }

        if (allWidgetIds.isEmpty()) {
            centerText {
                id("empty")
                content(R.string.manage_widgets_empty)
                paddingVertical(32)
                textColor(context.getColorCompat(R.color.text_secondary))
            }
        }

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
        verticalSpacerView {
            id("space_bottom")
            height(6)
        }
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
        val ratio = tickerWidgetRenderer.getWidgetRatio(config)
        val layoutNameRes = CoinTickerConfig.LayoutToString[config.layout]
            ?: R.string.coin_ticket_style_graph
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
                    CoinTickerSettingActivity.starterIntent(context, currentParams.config)
                )
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(viewModel: ManageWidgetViewModel): ManageWidgetController
    }
}
