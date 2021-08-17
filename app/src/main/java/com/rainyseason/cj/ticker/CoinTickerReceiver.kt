package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.goBackground
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import javax.inject.Inject

@Module
interface CoinTickerReceiverModule {
    @ContributesAndroidInjector
    fun provider(): CoinTickerReceiver
}

class CoinTickerReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var coinTickerHandler: CoinTickerHandler

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        goBackground {
            when (intent.action) {
                AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                    val ids = intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                    ids?.forEach {
                        coinTickerHandler.enqueueRefreshWidget(it)
                    }
                }
                Intent.ACTION_BOOT_COMPLETED -> {
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(
                            context,
                            CoinTickerProvider::class.java
                        )
                    )
                    ids.forEach { coinTickerHandler.enqueueRefreshWidget(widgetId = it) }
                }
            }
        }

    }
}