package com.rainyseason.cj.common

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.util.TypedValue
import androidx.core.content.ContextCompat
import androidx.core.graphics.applyCanvas

fun drawLinearGradient(context: Context): Bitmap {
    val size = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        20f,
        context.resources.displayMetrics
    ).toInt()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val sizeF = size.toFloat()
    val gradient = LinearGradient(
        sizeF / 2, 0f, sizeF / 2, sizeF,
        ContextCompat.getColor(context, R.color.transparent), Color.GREEN, Shader.TileMode.CLAMP
    )
    val p = Paint()
    p.isDither = true
    p.shader = gradient

    bitmap.applyCanvas {
        drawRect(Rect(0, 0, size, size), p)
    }
    return bitmap
}