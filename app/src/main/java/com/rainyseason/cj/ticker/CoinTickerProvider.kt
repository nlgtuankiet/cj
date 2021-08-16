package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.rainyseason.cj.data.local.CoinTickerRepository
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.runBlocking
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
        Timber.d("thread: ${Thread.currentThread().name} onReceive")
        AndroidInjection.inject(this, context)
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val currentIds = runBlocking { repository.allKey() }
        val updateIds = appWidgetIds.toList().filter { widgetId ->
            currentIds.any { it.contains(it) }
        }

        Timber.d("thread: ${Thread.currentThread().name} updateIds = $updateIds ")
        updateIds.forEach { widgetId ->
            val request = OneTimeWorkRequestBuilder<RefreshCoinTickerWorker>()
                .setInputData(
                    Data.Builder().putInt(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId).build()
                )
                .build()
            workManager.enqueue(request)
        }
    }
}