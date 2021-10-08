package com.rainyseason.cj.common

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class NoopWorker constructor(
    context: Context,
    parameters: WorkerParameters,
) : Worker(context, parameters) {
    override fun doWork(): Result {
        return Result.success()
    }
}
