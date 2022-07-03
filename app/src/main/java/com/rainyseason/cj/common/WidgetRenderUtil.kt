package com.rainyseason.cj.common

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.core.os.BuildCompat
import com.rainyseason.cj.R
import com.rainyseason.cj.common.model.Theme
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRenderUtil @Inject constructor(
    private val context: Context,
) {

    private fun <T> select(theme: Theme, light: T, dark: T): T {
        if (theme == Theme.Light || theme == Theme.MaterialYouLight) {
            return light
        }
        if (theme == Theme.Dark || theme == Theme.MaterialYouDark) {
            return dark
        }
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (mode == Configuration.UI_MODE_NIGHT_YES) {
            return dark
        }
        return light
    }

    private inline fun <T> select(
        theme: Theme,
        light: T,
        dark: T,
        crossinline muLight: () -> T,
        crossinline muDark: () -> T,
    ): T {
        return if (theme.isMaterialYou && BuildCompat.isAtLeastS()) {
            select(theme, muLight(), muDark())
        } else {
            select(theme, light, dark)
        }
    }

    fun getDividerColor(theme: Theme): Int {
        return select(
            theme,
            R.color.gray_300,
            R.color.gray_700,
            { android.R.color.system_neutral1_200 },
            { android.R.color.system_neutral1_800 },
        ).let { context.getColorCompat(it) }
    }

    fun getBackgroundResource(theme: Theme): Int {
        return select(
            theme,
            R.drawable.widget_background_light,
            R.drawable.widget_background_dark,
            { R.drawable.widget_background_mu_light },
            { R.drawable.widget_background_mu_dark },
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun RemoteViews.setBackgroundTint(
        id: Int,
        color: Int,
    ) {
        setColorStateList(id, "setBackgroundTintList", ColorStateList.valueOf(color))
    }

    fun setBackground(
        remoteViews: RemoteViews,
        id: Int,
        theme: Theme,
        transparency: Int
    ) {
        remoteViews.setBackgroundResource(
            id,
            getBackgroundResource(theme)
        )
        if (!BuildCompat.isAtLeastS()) {
            return
        }
        val baseColor = select(
            theme,
            R.color.gray_50,
            R.color.gray_900,
            { android.R.color.system_accent2_50 },
            { android.R.color.system_accent2_800 },
        ).let { context.getColorCompat(it) }
        val alpha = ((100 - transparency.toDouble()) / 100 * 255).toInt()
        val alphaColor = Color.argb(
            alpha,
            Color.red(baseColor),
            Color.green(baseColor),
            Color.blue(baseColor),
        )
        remoteViews.setBackgroundTint(id, alphaColor)
    }

    fun getBackgroundPositiveResource(theme: Theme, isPositive: Boolean): Int {
        return if (isPositive) {
            select(
                theme,
                R.drawable.coin_ticker_background_positive_light,
                R.drawable.coin_ticker_background_positive_dark
            )
        } else {
            select(
                theme,
                R.drawable.coin_ticker_background_negative_light,
                R.drawable.coin_ticker_background_negative_dark
            )
        }
    }

    @ColorInt
    private fun blend(@ColorRes a: Int, @ColorRes b: Int, ratio: Float = 0.5f): Int {
        return ColorUtils.blendARGB(
            context.getColorCompat(a),
            context.getColorCompat(b),
            ratio
        )
    }

    private inline fun <T> selectPositive(
        theme: Theme,
        isPositive: Boolean,
        positive: T,
        negative: T,
        crossinline muPositive: () -> T,
        crossinline muNegative: () -> T,
    ): T {
        return if (theme.isMaterialYou && BuildCompat.isAtLeastS()) {
            if (isPositive) {
                muPositive()
            } else {
                muNegative()
            }
        } else {
            if (isPositive) {
                positive
            } else {
                negative
            }
        }
    }

    fun getTickerLineColor(theme: Theme, isPositive: Boolean): Int {
        return selectPositive(
            theme,
            isPositive,
            context.getColorCompat(R.color.ticket_line_green),
            context.getColorCompat(R.color.ticket_line_red),
            {
                context.getColorCompat(R.color.ticket_line_green)
            },
            {
                context.getColorCompat(R.color.ticket_line_red)
            }
        )
    }

    fun getChangePercentColor(
        theme: Theme,
        amount: Double = 1.0,
        isPositiveOverride: Boolean? = null
    ): Int {
        return selectPositive(
            theme,
            isPositiveOverride ?: (amount > 0),
            context.getColorCompat(R.color.green_700),
            context.getColorCompat(R.color.red_600),
            {
                context.getColorCompat(R.color.green_700)
            },
            {
                context.getColorCompat(R.color.red_600)
            }
        )
    }

    fun getBackgroundColor(theme: Theme): Int {
        return select(
            theme,
            R.color.gray_50,
            R.color.gray_900,
            { android.R.color.system_accent2_50 },
            { android.R.color.system_accent2_800 }
        ).let { context.getColorCompat(it) }
    }

    fun getTextSecondaryColor(theme: Theme): Int {
        return select(
            theme,
            R.color.gray_500,
            R.color.text_secondary,
            { android.R.color.system_accent2_500 },
            { android.R.color.system_accent2_400 }
        ).let { context.getColorCompat(it) }
    }

    fun getTextPrimaryColor(theme: Theme): Int {
        return select(
            theme,
            R.color.gray_900,
            R.color.gray_50,
            { android.R.color.system_accent1_900 },
            { android.R.color.system_accent1_100 }
        ).let { context.getColorCompat(it) }
    }
}
