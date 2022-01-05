package com.rainyseason.cj.widget.manage

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import com.rainyseason.cj.common.fragment
import com.rainyseason.cj.common.update
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerDisplayData
import com.rainyseason.cj.widget.watch.WatchConfig
import com.rainyseason.cj.widget.watch.WatchDisplayData
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ticker

data class ManageWidgetState(
    val id: Int = 0,
    val tickerConfigs: Map<Int, Async<CoinTickerConfig>> = emptyMap(),
    val tickerDisplayData: Map<Int, Async<CoinTickerDisplayData>> = emptyMap(),
    val watchConfig: Map<Int, Async<WatchConfig>> = emptyMap(),
    val watchDisplayData: Map<Int, Async<WatchDisplayData>> = emptyMap(),
): MavericksState

class ManageWidgetViewModel @AssistedInject constructor(
    initState: ManageWidgetState,
    private val appWidgetManager: AppWidgetManager,
    private val coinTickerRepository: CoinTickerRepository,
    private val context: Context,
) : MavericksViewModel<ManageWidgetState>(initState) {

    init {
        reload()
    }

    private fun reload() {
        loadWidgets()
    }

    private val getConfigJobs = mutableMapOf<Int, Job?>()
    private val getDataJobs = mutableMapOf<Int, Job?>()

    private fun loadWidgets() {
        CoinTickerConfig.Layout.clazzToLayout.keys.forEach { clazz ->
            appWidgetManager.getAppWidgetIds(ComponentName(context, clazz))
                .filter { it != 0 }
                .forEach { widgetId ->
                    getConfigJobs[widgetId]?.cancel()
                    getConfigJobs[widgetId] = coinTickerRepository.getConfigStream(widgetId)
                        .execute {
                            copy(tickerConfigs = tickerConfigs.update { put(widgetId, it) })
                        }

                    getDataJobs[widgetId]?.cancel()
                    getDataJobs[widgetId] = coinTickerRepository.getDisplayDataStream(widgetId)
                        .execute {
                            copy(tickerDisplayData = tickerDisplayData.update { put(widgetId, it) })
                        }
                }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initState: ManageWidgetState): ManageWidgetViewModel
    }

    companion object : MavericksViewModelFactory<ManageWidgetViewModel, ManageWidgetState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: ManageWidgetState
        ): ManageWidgetViewModel {
            return viewModelContext.fragment<ManageWidgetFragment>().viewModelFactory.create(state)
        }
    }
}