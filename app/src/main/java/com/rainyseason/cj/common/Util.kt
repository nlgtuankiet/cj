@file:Suppress("unused")

package com.rainyseason.cj.common

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.PowerManager
import android.util.SizeF
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
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.os.BuildCompat
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
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestFutureTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.GlideRequest
import com.rainyseason.cj.R
import com.rainyseason.cj.coinselect.CoinSelectResult
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.featureflag.DebugFlag
import com.rainyseason.cj.featureflag.isEnable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import retrofit2.HttpException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
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

fun Context.dpToPx(value: Number): Int {
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

@ColorInt
fun Context.resolveColorAttr(@AttrRes id: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(id, typedValue, true)
    return typedValue.data
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

fun AlertDialog.Builder.setResetButton(onButtonClick: () -> Unit): AlertDialog.Builder {
    setNeutralButton(R.string.reset) { dialog, _ ->
        dialog.dismiss()
        onButtonClick.invoke()
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

fun Int.addFlagMutable(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this or PendingIntent.FLAG_MUTABLE
    } else {
        this
    }
}

fun Int.asColorStateList(): ColorStateList {
    return ColorStateList.valueOf(this)
}

fun Bundle.getAppWidgetSizes(): ArrayList<SizeF>? {
    return if (BuildCompat.isAtLeastS()) {
        getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES)
    } else {
        null
    }
}

fun AppWidgetManager.getTrackingParams(widgetId: Int): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    val hasAppWidgetSizes = if (BuildCompat.isAtLeastS()) {
        val option = getAppWidgetOptions(widgetId)
        !option.getAppWidgetSizes().isNullOrEmpty()
    } else {
        false
    }
    result["has_app_widget_sizes"] = hasAppWidgetSizes
    return result
}

val View.viewScope: CoroutineScope
    get() {
        getTag(R.id.view_scope)?.let {
            if (it is CoroutineScope) {
                return it
            } else {
                if (BuildConfig.DEBUG) {
                    error("Check why the value of KEY_VIEW_SCOPE is ${it.javaClass.name}")
                }
            }
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        setTag(R.id.view_scope, scope)

        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {}

            override fun onViewDetachedFromWindow(view: View) {
                removeOnAttachStateChangeListener(this)
                setTag(R.id.view_scope, null)
                scope.cancel()
            }
        })

        return scope
    }

suspend fun <T> GlideRequest<T>.await(context: Context): T {
    return suspendCancellableCoroutine { cont ->
        val target = object : RequestFutureTarget<T>(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL) {
            override fun onResourceReady(resource: T, transition: Transition<in T>?) {
                if (resource != null) {
                    super.onResourceReady(resource, transition)
                    cont.resume(resource)
                } else {
                    cont.resumeWithException(Exception("Null resource"))
                }
            }

            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<T>?,
                isFirstResource: Boolean
            ): Boolean {
                cont.resumeWithException(e ?: Exception("Unknown error"))
                return super.onLoadFailed(e, model, target, isFirstResource)
            }

            override fun onLoadCleared(placeholder: Drawable?) {
                super.onLoadCleared(placeholder)
                cont.cancel()
            }
        }
        cont.invokeOnCancellation {
            GlideApp.with(context).clear(target)
        }
        into(target)
    }
}

fun AppWidgetManager.isRequestPinAppWidgetSupportedCompat(): Boolean {
    if (DebugFlag.FORCE_NOT_SUPPORT_WIDGET_PIN.isEnable) {
        return false
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        isRequestPinAppWidgetSupported
    } else {
        false
    }
}

fun CoinSelectResult.asCoin(): Coin {
    return Coin(id = coinId, backend = backend, network = network, dex = dex)
}
