package com.rainyseason.cj.ticker

import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.common.putArgs
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewArgs
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewFragment
import javax.inject.Inject

@ActivityScope
class CoinTickerNavigator @Inject constructor(
    private val activity: CoinTickerSettingActivity
) {

    fun moveToPreview(coinId: String) {
        val widgetId = activity.getWidgetId()!!
        val args = CoinTickerPreviewArgs(
            widgetId = widgetId,
            coinId = coinId,
        )
        activity.supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, CoinTickerPreviewFragment().putArgs(args))
            .addToBackStack(null)
            .commit()
    }
}