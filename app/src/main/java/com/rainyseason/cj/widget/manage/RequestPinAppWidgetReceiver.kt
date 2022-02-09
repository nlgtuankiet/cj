package com.rainyseason.cj.widget.manage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.common.widgetId
import com.rainyseason.cj.ticker.CoinTickerConfig
import com.rainyseason.cj.ticker.CoinTickerLayout
import com.rainyseason.cj.ticker.CoinTickerSettingActivity
import timber.log.Timber

abstract class RequestPinAppWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val widgetId = intent.widgetId() ?: return
        Timber.d("widgetId: $widgetId")
        goBackground {
            process(context, widgetId)
        }
    }

    abstract suspend fun process(context: Context, widgetId: Int)
}

class RequestPinTickerWidgetReceiver : RequestPinAppWidgetReceiver() {
    override suspend fun process(context: Context, widgetId: Int) {
        val config = CoinTickerConfig.DEFAULT_FOR_PREVIEW.copy(
            widgetId = widgetId,
            layout = CoinTickerLayout.fromWidgetId(context, widgetId),
        )
        context.coreComponent.coinTickerRepository.setConfig(widgetId, config)
        val settingIntent = CoinTickerSettingActivity.starterIntent(context, widgetId, true)
        settingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(settingIntent)
        context.coreComponent.coinTickerHandler.enqueueRefreshWidget(widgetId, config)
    }
}
