package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.common.putArgs
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewArgs
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewFragment
import javax.inject.Inject

@ActivityScope
class CoinTickerNavigator @Inject constructor(
    private val activity: CoinTickerSettingActivity,
    private val appWidgetManager: AppWidgetManager,
) {

    fun moveToPreview(coinId: String) {
        val widgetId = activity.getWidgetId()!!
        val layout =
            when (val layoutRes = appWidgetManager.getAppWidgetInfo(widgetId).initialLayout) {
                R.layout.widget_coin_ticker_2x2_default -> CoinTickerConfig.Layout.DEFAULT
                R.layout.widget_coin_ticker_2x2_graph -> CoinTickerConfig.Layout.GRAPH
                R.layout.widget_coin_ticker_2x2_coin360 -> CoinTickerConfig.Layout.COIN360
                else -> error("Unknown layout for $layoutRes")
            }
        val args = CoinTickerPreviewArgs(
            widgetId = widgetId,
            coinId = coinId,
            layout = layout,
        )
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CoinTickerPreviewFragment().putArgs(args))
            .addToBackStack(null)
            .commit()
    }
}