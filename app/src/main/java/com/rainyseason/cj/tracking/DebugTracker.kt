package com.rainyseason.cj.tracking

import timber.log.Timber
import javax.inject.Inject

class DebugTracker @Inject constructor() : SyncTracker {
    override fun log(event: Event) {
        when (event) {
            is KeyParamsEvent -> logKeyParamsEvent(event)
        }
    }

    private fun logKeyParamsEvent(event: KeyParamsEvent) {
        val params = event.params.toList().sortedBy { it.first }
        Timber.d("""Log "${event.key}" values $params""")
    }
}
