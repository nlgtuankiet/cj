package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.work.WorkManager
import com.rainyseason.cj.data.local.CoinTickerRepository
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
    lateinit var repository: CoinTickerRepository

    @Inject
    lateinit var workManager: WorkManager

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("thread: ${Thread.currentThread().name} onReceive, intent: $intent, extra: ${intent.extras}")
        AndroidInjection.inject(this, context)
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Timber.d("thread: ${Thread.currentThread().name} updateIds = ${appWidgetIds.toList()} ")
    }

    override fun onAppWidgetOptionsChanged(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}