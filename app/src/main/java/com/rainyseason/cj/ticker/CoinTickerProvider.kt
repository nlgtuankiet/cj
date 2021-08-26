package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.rainyseason.cj.common.goBackground
import com.rainyseason.cj.common.logString
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import timber.log.Timber
import javax.inject.Inject


@Module
interface CoinTickerProviderModule {
    @ContributesAndroidInjector
    fun provider(): CoinTickerProvider
}

class CoinTickerProvider : AppWidgetProvider() {

    @Inject
    lateinit var coinTickerHandler: CoinTickerHandler

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("onReceive: ${intent.logString()}")
        AndroidInjection.inject(this, context)
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(
                        context,
                        CoinTickerProvider::class.java
                    )
                )
                goBackground {
                    ids.forEach { coinTickerHandler.enqueueRefreshWidget(widgetId = it) }
                }
            }
        }
    }


    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        goBackground {
            appWidgetIds.forEach {
                coinTickerHandler.enqueueRefreshWidget(it)
            }
        }
    }
}