package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.common.model.Exchange
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

    fun moveToPreview(coinId: String, exchangeId: String? = null) {
        activity.window?.decorView?.dismissKeyboard()
        val widgetId = activity.getWidgetId()!!
        val componentName = appWidgetManager.getAppWidgetInfo(widgetId)?.provider
            ?: ComponentName(activity, CoinTickerProviderNano::class.java)
        val layout = CoinTickerConfig.Layout.fromComponentName(componentName.className)
        val args = CoinTickerPreviewArgs(
            widgetId = widgetId,
            coinId = coinId,
            layout = layout,
            exchange = exchangeId?.let { Exchange.from(exchangeId) }
        )

        getNavHostController().navigate(R.id.preview, Bundle().putArgs(args))
    }
}
