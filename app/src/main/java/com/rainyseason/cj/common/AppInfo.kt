package com.rainyseason.cj.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.google.firebase.crashlytics.FirebaseCrashlytics

fun Context.appInfoIntent(): Intent? {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    intent.data = Uri.parse("package:${packageName}")
    intent.flags = intent.flags.or(Intent.FLAG_ACTIVITY_NEW_TASK)
    val hasActivity = packageManager.resolveActivity(intent, 0) != null
    if (!hasActivity) {
        FirebaseCrashlytics.getInstance().recordException(
            IllegalStateException("Unable to find app detail activity")
        )
        return null
    }
    return intent
}