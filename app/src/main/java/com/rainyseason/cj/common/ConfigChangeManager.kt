package com.rainyseason.cj.common

import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigChangeManager @Inject constructor() {
    private val listeners = mutableListOf<ConfigChangeListener>()
    private val handler = Handler(Looper.getMainLooper())

    fun registerListener(listener: ConfigChangeListener) {
        doOnMainThread {
            listeners.add(listener)
            listener.onConfigChange()
        }
    }

    fun notifyListeners() {
        doOnMainThread {
            listeners.forEach { it.onConfigChange() }
        }
    }

    private fun doOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() === Looper.getMainLooper()) {
            block.invoke()
        } else {
            handler.post { block.invoke() }
        }
    }
}

interface ConfigChangeListener {
    fun onConfigChange()
}
