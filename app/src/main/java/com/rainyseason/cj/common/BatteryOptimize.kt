package com.rainyseason.cj.common

import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rainyseason.cj.R
import com.rainyseason.cj.tracking.logKeyParamsEvent

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
