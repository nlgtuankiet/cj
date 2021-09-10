package com.rainyseason.cj.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.ModelCollector
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

fun Context.dpToPxF(value: Float): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics
    )
}


fun <T : Parcelable> Fragment.requireArgs(): T {
    return arguments!!.getParcelable<T>("args")!!
}

fun <T : Parcelable, F : Fragment> F.putArgs(args: T): F {
    arguments = Bundle().apply {
        putParcelable("args", args)
    }
    return this
}

@ColorInt
fun Context.getColorCompat(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}


fun AlertDialog.Builder.setCancelButton(): AlertDialog.Builder {
    setCancelable(true)
    setNegativeButton(android.R.string.cancel) { dialog, _ ->
        dialog.dismiss()
    }
    return this
}

fun <K, V> Map<K, V>.update(block: MutableMap<K, V>.() -> Unit): Map<K, V> {
    val new = toMutableMap()
    block.invoke(new)
    return new.toMap()
}


fun List<List<Double>>.changePercent(): Double {
    val open = first()[1]
    val last = last()[1]
    val diff = last - open
    return 1.0 * diff / open
}

fun RemoteViews.setBackgroundResource(@IdRes id: Int, @DrawableRes value: Int) {
    setInt(id, "setBackgroundResource", value)
}

fun buildModels(block: ModelCollector.() -> Unit): List<EpoxyModel<*>> {
    val models = mutableListOf<EpoxyModel<*>>()
    val collector = object  : ModelCollector {
        override fun add(model: EpoxyModel<*>) {
            models.add(model)
        }
    }
    block.invoke(collector)
    return models
}