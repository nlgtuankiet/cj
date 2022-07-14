package com.rainyseason.cj.common

import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rainyseason.cj.R
import com.rainyseason.cj.tracking.logKeyParamsEvent
import java.util.concurrent.TimeUnit

fun Fragment.saveOrShowWarning(
    refreshInterval: Long,
    onSave: () -> Unit
) {
    saveOrShowBatteryOptimize {
        saveOrShowShortRefreshWarning(refreshInterval) {
            onSave.invoke()
        }
    }
}

// temporarily disable because user want widget always up to date rather than save battery
@Suppress("UNREACHABLE_CODE", "UNUSED_PARAMETER")
fun Fragment.saveOrShowShortRefreshWarning(
    refreshInterval: Long,
    onSave: () -> Unit
) {
    onSave()
    return

    val context = requireContext()
    val tracker = context.coreComponent.tracker
    if (refreshInterval < TimeUnit.MINUTES.toMillis(15)) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.warning_refresh_interval)
            .setMessage(R.string.warning_refresh_interval_description)
            .setPositiveButton(R.string.coin_ticker_preview_save_widget_anyway) { _, _ ->
                tracker.logKeyParamsEvent(
                    "short_refresh_impression",
                    mapOf("action" to "save_anyway")
                )
                onSave()
            }
            .setCancelButton()
            .show()
    } else {
        onSave()
    }
}

fun Fragment.saveOrShowBatteryOptimize(
    onSave: () -> Unit
) {
    val context = requireContext()
    val tracker = context.coreComponent.tracker
    val isInBatterySaver = context.isInBatteryOptimize()
    if (isInBatterySaver) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.coin_ticker_preview_battery_saver_warning_dialog_title)
            .setMessage(R.string.coin_ticker_preview_battery_saver_warning)
            .setPositiveButton(R.string.coin_ticker_preview_save_widget_anyway) { _, _ ->
                tracker.logKeyParamsEvent(
                    "battery_saver_impression",
                    mapOf("action" to "save_anyway")
                )
                onSave()
            }
            .apply {
                val intent = requireContext().appInfoIntent()
                if (intent != null) {
                    setNegativeButton(R.string.coin_ticker_preview_go_to_app_info) { _, _ ->
                        tracker.logKeyParamsEvent(
                            "battery_saver_impression",
                            mapOf("action" to "open_app_info")
                        )
                        startActivity(intent)
                    }
                }
            }
            .show()
    } else {
        onSave()
    }
}
