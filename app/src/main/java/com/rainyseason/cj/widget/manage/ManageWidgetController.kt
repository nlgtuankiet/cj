package com.rainyseason.cj.widget.manage

import androidx.navigation.findNavController
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.CoinTickerSettingActivity
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.widget.manage.view.WidgetRenderParam
import com.rainyseason.cj.widget.manage.view.widgetView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ManageWidgetController @AssistedInject constructor(
    @Assisted private val viewModel: ManageWidgetViewModel,
    private val tickerWidgetRenderer: TickerWidgetRenderer,
) : AsyncEpoxyController() {

    override fun buildModels() {
        val state = withState(viewModel) { it }
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
                // todo
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
        widgetView {
            id(config.widgetId)
            title(displayData.symbol)
            subtitle(config.backend.displayName)
            tickerWidgetParams(
                WidgetRenderParam(
                    ratio,
                    params
                )
            )
            isRefreshing(refreshing)
            onRefreshClickListener { _ ->
                viewModel.refreshTickerWidget(widgetId)
            }
            onClickListener { model, parentView, clickedView, position ->
                val context = clickedView.context
                val tickerConfig = model.tickerWidgetParams().coinTickerRenderParams?.config
                    ?: return@onClickListener
                context.startActivity(
                    CoinTickerSettingActivity.starterIntent(context, tickerConfig)
                )
            }

        }
    }

    @AssistedFactory
    interface Factory {
        fun create(viewModel: ManageWidgetViewModel): ManageWidgetController
    }
}