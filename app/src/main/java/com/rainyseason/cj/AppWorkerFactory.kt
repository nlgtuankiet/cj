package com.rainyseason.cj

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.rainyseason.cj.ticker.RefreshCoinTickerWorker
import com.rainyseason.cj.ticker.RefreshWatchWidgetWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppWorkerFactory @Inject constructor(
    private val refreshCoinTickerWorkerFactory: RefreshCoinTickerWorker.Factory,
    private val refreshWatchWidgetWorkerFactory: RefreshWatchWidgetWorker.Factory,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            RefreshCoinTickerWorker::class.java.name ->
                refreshCoinTickerWorkerFactory.create(appContext, workerParameters)
            RefreshWatchWidgetWorker::class.java.name ->
                refreshWatchWidgetWorkerFactory.create(appContext, workerParameters)
            else -> null
        }
    }
}
