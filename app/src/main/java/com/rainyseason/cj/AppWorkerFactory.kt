package com.rainyseason.cj

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.rainyseason.cj.ticker.RefreshCoinTickerWorker
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppWorkerFactory @Inject constructor(
    private val refreshCoinTickerWorkerFactory: RefreshCoinTickerWorker.Factory
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            RefreshCoinTickerWorker::class.java.name ->
                refreshCoinTickerWorkerFactory.create(appContext, workerParameters)
            else -> null
        }
    }
}