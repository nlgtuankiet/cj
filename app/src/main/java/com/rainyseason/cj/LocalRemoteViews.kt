package com.rainyseason.cj

import android.app.Activity
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import java.lang.ref.WeakReference

class LocalRemoteViews(
    activity: Activity,
    @IdRes containerId: Int,
    @LayoutRes layoutId: Int,
) : RemoteViews(activity.packageName, layoutId) {

    private val containerRef: WeakReference<ViewGroup>

    init {
        val container = activity.findViewById<ViewGroup>(containerId)
        View.inflate(activity, layoutId, container)
        containerRef = WeakReference(container)
    }

    override fun setTextViewText(viewId: Int, text: CharSequence) {
        invokeOn<TextView>(viewId) { it.text = text }
    }

    override fun setViewVisibility(viewId: Int, visibility: Int) {
        invokeOn<View>(viewId) { it.visibility = visibility }
    }

    override fun setImageViewResource(viewId: Int, srcId: Int) {
        invokeOn<ImageView>(viewId) { it.setImageResource(srcId) }
    }

    override fun setImageViewBitmap(viewId: Int, bitmap: Bitmap) {
        invokeOn<ImageView>(viewId) { it.setImageBitmap(bitmap) }
    }

    override fun setTextViewTextSize(viewId: Int, units: Int, size: Float) {
        invokeOn<TextView>(viewId) { it.setTextSize(units, size) }
    }

    override fun setTextColor(viewId: Int, color: Int) {
        invokeOn<TextView>(viewId) { it.setTextColor(color) }
    }

    override fun setInt(viewId: Int, methodName: String?, value: Int) {
        when (methodName) {
            "setBackgroundResource" -> invokeOn<View>(viewId) { it.setBackgroundResource(value) }
        }
    }

    private inline fun <reified Y : View> invokeOn(
        @IdRes viewId: Int,
        crossinline block: (Y) -> Unit,
    ) {
        containerRef.get()?.findViewById<Y>(viewId)?.let(block)
    }

}