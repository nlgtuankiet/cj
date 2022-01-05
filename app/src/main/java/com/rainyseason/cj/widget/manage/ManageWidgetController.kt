package com.rainyseason.cj.widget.manage

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ManageWidgetController @AssistedInject constructor(
    @Assisted private val viewModel: ManageWidgetViewModel,
    private val tickerWidgetRenderer: TickerWidgetRenderer
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
        val widgetSize = tickerWidgetRenderer.getWidgetSize()
    }

    @AssistedFactory
    interface Factory {
        fun create(viewModel: ManageWidgetViewModel): ManageWidgetController
    }
}