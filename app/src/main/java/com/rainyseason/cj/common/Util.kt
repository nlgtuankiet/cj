@file:Suppress("unused")

package com.rainyseason.cj.common

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.PowerManager
import android.util.TypedValue
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.RemoteViews
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.ModelCollector
import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.ViewModelContext
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import retrofit2.HttpException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.math.abs

/**
 * Launches a new coroutine and repeats `block` every time the Fragment's viewLifecycleOwner
 * is in and out of `minActiveState` lifecycle state.
 */
inline fun Fragment.launchAndRepeatWithViewLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.RESUMED,
    crossinline block: suspend CoroutineScope.() -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.lifecycle.repeatOnLifecycle(minActiveState) {
            block()
        }
    }
}

inline fun ComponentActivity.launchAndRepeatWithLifecycle(
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline block: suspend CoroutineScope.() -> Unit,
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

fun ViewGroup.inflateAndAdd(@LayoutRes layoutRes: Int): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, true)
}

val View.inflater: LayoutInflater
    get() = LayoutInflater.from(context)

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
        value,
        resources.displayMetrics
    )
}

fun <T : Parcelable> Fragment.requireArgs(): T {
    return arguments!!.getParcelable("args")!!
}

fun <T : Parcelable, F : Fragment> F.putArgs(args: T): F {
    arguments = Bundle().putArgs(args)
    return this
}

fun <T : Parcelable> Bundle.putArgs(args: T): Bundle {
    putParcelable("args", args)
    return this
}

fun Parcelable.asArgs(): Bundle {
    return Bundle().putArgs(this)
}

@ColorInt
fun Context.getColorCompat(@ColorRes id: Int): Int {
    return ContextCompat.getColor(this, id)
}

fun Context.getDrawableCompat(@DrawableRes id: Int): Drawable? {
    return ContextCompat.getDrawable(this, id)
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

fun List<List<Double>>.changePercent(): Double? {
    if (this.isEmpty()) {
        return null
    }
    val open = first()[1]
    val last = last()[1]
    val diff = last - open
    if (open == 0.0) {
        return null
    }
    return 1.0 * diff / open
}

fun RemoteViews.setBackgroundColor(@IdRes id: Int, @ColorInt value: Int) {
    setInt(id, "setBackgroundColor", value)
}

fun RemoteViews.setBackgroundResource(@IdRes id: Int, @DrawableRes value: Int) {
    setInt(id, "setBackgroundResource", value)
}

fun buildModels(block: ModelCollector.() -> Unit): List<EpoxyModel<*>> {
    val models = mutableListOf<EpoxyModel<*>>()
    val collector = object : ModelCollector {
        override fun add(model: EpoxyModel<*>) {
            models.add(model)
        }
    }
    block.invoke(collector)
    return models
}

fun Throwable.getUserErrorMessage(context: Context): CharSequence {
    if (this is UnknownHostException) {
        return context.getString(R.string.error_no_network)
    }
    if (this is HttpException && this.code() == 429) {
        return context.getString(R.string.error_rate_limit)
    }
    return context.getString(R.string.error_unknown)
}

fun Context.activity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.dismissKeyboard() {
    activity()?.window?.decorView?.dismissKeyboard()
}

fun View.showKeyboard() {
    val imm: InputMethodManager? =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.showSoftInput(this, 0)
    ViewCompat.getWindowInsetsController(this)
        ?.show(WindowInsetsCompat.Type.ime())
}

fun View.dismissKeyboard() {
    val imm: InputMethodManager? =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
    imm?.hideSoftInputFromWindow(windowToken, 0)
    ViewCompat.getWindowInsetsController(this)
        ?.hide(WindowInsetsCompat.Type.ime())
}

data class VerticalTextPadding(val top: Int, val bottom: Int)

fun TextView.verticalPadding(): VerticalTextPadding {
    val bounds = Rect()
    paint.getTextBounds("A", 0, 1, bounds)
    val bottom = paint.fontMetricsInt.run { abs(bottom) }
    val top = height - bounds.height() - bottom
    return VerticalTextPadding(top = top, bottom = bottom)
}

fun Context.inflater(): LayoutInflater {
    val themeContext = ContextThemeWrapper(this, R.style.Theme_CryptoJet)
    return LayoutInflater.from(themeContext)
}

fun Context.isInBatteryOptimize(): Boolean {
    val powerManager = getSystemService<PowerManager>()!!
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        powerManager.isPowerSaveMode && !powerManager.isIgnoringBatteryOptimizations(packageName)
    } else {
        powerManager.isPowerSaveMode
    }
}

fun View.clearHapticFeedback() {
    setTag(R.id.haptic_feedback, null)
}

fun View.hapticFeedbackIfChanged(key: Any) {
    val oldValue = getTag(R.id.haptic_feedback)
    if (key != oldValue) {
        setTag(R.id.haptic_feedback, key)
        performHapticFeedback(
            HapticFeedbackConstants.VIRTUAL_KEY,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}

fun View.hapticFeedback() {
    performHapticFeedback(
        HapticFeedbackConstants.VIRTUAL_KEY,
        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
    )
}

fun Bundle.getWidgetId(): Int? {
    val widgetId = getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        return null
    }
    return widgetId
}

fun View.setOnClickToNavigateBack() {
    setOnClickListener { view ->
        view.findNavController().navigateUp()
    }
}

fun List<List<Double>>.reverseValue(): List<List<Double>> {
    return mapIndexed { index, point ->
        listOf(point[0], this[this.size - 1 - index][1])
    }
}

fun List<List<Double>>.findApproxIndex(approxTime: Double): Int {
    var low = 0
    var high = size - 1
    var minDelta = Double.MAX_VALUE
    var minIndex = 0

    while (low <= high) {
        val mid = (low + high).ushr(1) // safe from overflows
        val midVal = this[mid]
        val time = midVal[0]
        val delta = abs(time - approxTime)
        if (delta < minDelta) {
            minIndex = mid
            minDelta = delta
        }

        val cmp = time.compareTo(approxTime)

        when {
            cmp < 0 -> low = mid + 1
            cmp > 0 -> high = mid - 1
            else -> return mid
        } // key found
    }
    return minIndex
}

fun <F : Fragment> ViewModelContext.fragment(): F =
    (this as FragmentViewModelContext).fragment()

@Suppress("NOTHING_TO_INLINE")
inline fun <T> T?.notNull(): T {
    return checkNotNull(this)
}

@OptIn(ExperimentalCoroutinesApi::class)
fun FirebaseAuth.isUserLoginFlow(): Flow<Boolean> {
    return callbackFlow {
        trySend(currentUser != null)
        val listener = FirebaseAuth.AuthStateListener { trySend(currentUser != null) }
        addAuthStateListener(listener)
        awaitClose {
            removeAuthStateListener(listener)
        }
    }
}

suspend fun View.showWithAnimation() {
    animateShow(this)
}

suspend fun View.hideWithAnimation() {
    animateShow(this, true)
}

private suspend fun animateShow(view: View, reverse: Boolean = false) {
    // 0 complete transparent
    // 1 show 100%
    if (reverse) {
        view.alpha = 1f
    } else {
        view.alpha = 0f
    }
    suspendCancellableCoroutine<Unit> { cont ->
        val animator = view.animate()
            .apply {
                if (reverse) {
                    alpha(0f)
                } else {
                    alpha(1f)
                }
            }
            .setDuration(300)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                cont.resume(Unit)
            }
        cont.invokeOnCancellation { animator.cancel() }
        animator.start()
    }
}

fun Intent.widgetId(): Int? {
    val widgetId = extras?.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
    if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
        return null
    }
    return widgetId
}

suspend fun RecyclerView.awaitScrollState(state: Int) {
    yield()
    if (state == scrollState) {
        return
    }
    suspendCancellableCoroutine<Unit> { cont ->
        val listener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == state) {
                    removeOnScrollListener(this)
                    cont.resume(Unit)
                }
            }
        }
        cont.invokeOnCancellation {
            removeOnScrollListener(listener)
        }
        addOnScrollListener(listener)
    }
}
