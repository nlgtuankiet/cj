package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Module
interface CoinTickerHandlerModule {
    @ContributesAndroidInjector
    fun provider(): CoinTickerHandler
}

class CoinTickerHandler : BroadcastReceiver() {

    @Inject
    lateinit var workManager: WorkManager

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        val action = intent.action
        when (action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val ids = intent.extras?.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ids?.forEach {
                    forceUpdateWidget(it)
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(
                        context,
                        CoinTickerProvider::class.java
                    )
                )
                ids.forEach { forceUpdateWidget(widgetId = it) }
            }
        }
    }

    private fun forceUpdateWidget(widgetId: Int) {
        val request = PeriodicWorkRequestBuilder<RefreshCoinTickerWorker>(15, TimeUnit.MINUTES)
            .setInputData(
                Data.Builder().putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId).build()
            )
            .build()
        workManager.enqueueUniquePeriodicWork(
            "refresh_${widgetId}",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }
}