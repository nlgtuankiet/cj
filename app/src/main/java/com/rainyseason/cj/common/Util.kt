package com.rainyseason.cj.common

import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Launches a new coroutine and repeats `block` every time the Fragment's viewLifecycleOwner
 * is in and out of `minActiveState` lifecycle state.
 */
inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}


inline fun ComponentActivity.launchAndRepeatWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit
) {
    lifecycleScope.launch {
        lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}


fun Intent.logString(): String {
    val intent = this
    val string = buildString {
        append("Intent { ")
        append("action = ${intent.action} ")
        val map = mutableMapOf<String, Any?>()
        extras?.let { e ->
            e.keySet().forEach { k ->
                map[k] = e.get(k)
            }
        }
        append("extra = $map ")
        append("}")
    }
    return string
}


fun ViewGroup.inflateAndAdd(@LayoutRes layoutRes: Int) {
    LayoutInflater.from(context).inflate(layoutRes, this, true)
}


fun Context.dpToPx(value: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    ).toInt()
}