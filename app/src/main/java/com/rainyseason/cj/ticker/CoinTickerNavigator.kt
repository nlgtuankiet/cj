package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.common.putArgs
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewArgs
import javax.inject.Inject

@ActivityScope
class CoinTickerNavigator @Inject constructor(
    private val activity: CoinTickerSettingActivity,
    private val appWidgetManager: AppWidgetManager,
) {

    private fun getNavHostController(): NavController {
        val navHostFragment = activity.supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController
    }

    fun moveToPreview(coinId: String) {
        activity.window?.decorView?.dismissKeyboard()
        val widgetId = activity.getWidgetId()!!
        val layoutRes = appWidgetManager.getAppWidgetInfo(widgetId)?.initialLayout
            ?: R.layout.widget_coin_ticker_2x2_default
        val layout = when (layoutRes) {
            R.layout.widget_coin_ticker_2x2_default -> CoinTickerConfig.Layout.DEFAULT
            R.layout.widget_coin_ticker_2x2_graph -> CoinTickerConfig.Layout.GRAPH
            R.layout.widget_coin_ticker_2x2_coin360 -> CoinTickerConfig.Layout.COIN360
            R.layout.widget_coin_ticker_1x1_coin360_mini -> CoinTickerConfig.Layout.COIN360_MINI
            R.layout.widget_coin_ticker_2x1_mini -> CoinTickerConfig.Layout.MINI
            else -> error("Unknown layout for $layoutRes")
        }
        val args = CoinTickerPreviewArgs(
            widgetId = widgetId,
            coinId = coinId,
            layout = layout,
        )

        getNavHostController().navigate(R.id.preview, Bundle().putArgs(args))
    }
}
