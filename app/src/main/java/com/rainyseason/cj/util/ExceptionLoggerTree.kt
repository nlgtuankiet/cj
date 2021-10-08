package com.rainyseason.cj.util

import timber.log.Timber
import javax.inject.Inject

class ExceptionLoggerTree @Inject constructor() : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        t?.printStackTrace()
    }
}
