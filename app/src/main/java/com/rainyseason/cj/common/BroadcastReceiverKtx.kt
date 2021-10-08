package com.rainyseason.cj.common

import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun BroadcastReceiver.goBackground(
    coroutineScope: CoroutineScope = CoroutineScope(Job() + Dispatchers.IO),
    block: suspend () -> Unit,
) {
    val result = goAsync()
    coroutineScope.launch {
        try {
            block()
        } finally {
            // Always call finish(), even if the coroutineScope was cancelled
            result.finish()
        }
    }
}
