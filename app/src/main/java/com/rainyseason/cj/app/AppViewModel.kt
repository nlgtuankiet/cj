package com.rainyseason.cj.app

import android.appwidget.AppWidgetManager
import android.content.ComponentCallbacks
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.rainyseason.cj.data.database.kv.KeyValueStore
import com.rainyseason.cj.ticker.CoinTickerHandler
import com.rainyseason.cj.ticker.CoinTickerProvider
import com.rainyseason.cj.widget.watch.WatchWidgetHandler
import com.rainyseason.cj.widget.watch.WatchWidgetLayout
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class AppState(
    val int: Int = 0,
) : MavericksState

@Singleton
class AppViewModel @Inject constructor(
    private val context: Context,
    private val keyValueStore: KeyValueStore,
    private val appWidgetManager: AppWidgetManager,
    private val watchWidgetHandler: WatchWidgetHandler,
    private val coinTickerHandler: CoinTickerHandler,
) : MavericksViewModel<AppState>(AppState()), ComponentCallbacks {

    private val configEvent = Channel<Configuration>()

    init {
        observeUiMode()
    }

    private fun observeUiMode() {
        viewModelScope.launch {
            configEvent.receiveAsFlow()
                .collect { config ->
                    onUiModeChange(config)
                }
        }
    }

    private suspend fun onUiModeChange(config: Configuration) {
        val oldUiMode = keyValueStore.getLong("ui_mode_night")
        val newUiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK).toLong()
        Timber.d("oldUiMode $oldUiMode newUiMode $newUiMode")
        keyValueStore.setLong("ui_mode_night", newUiMode)
        if (newUiMode != oldUiMode) {
            // ui mode may change from light to dark so widget need to re-render
            renderAllWidget()
        }
    }

    private suspend fun renderAllWidget() {
        CoinTickerProvider.PROVIDERS.forEach { clazz ->
            val widgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, clazz))
            widgetIds.forEach { widgetId ->
                coinTickerHandler.rerender(widgetId)
            }
        }

        WatchWidgetLayout.values().forEach { layout ->
            val widgetIds = appWidgetManager
                .getAppWidgetIds(ComponentName(context, layout.providerName))
            widgetIds.forEach { widgetId ->
                watchWidgetHandler.rerender(widgetId)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Timber.d("onConfigurationChanged $newConfig")
        viewModelScope.launch {
            configEvent.send(newConfig)
        }
    }

    override fun onLowMemory() {
    }
}
