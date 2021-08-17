package com.rainyseason.cj.ticker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.RemoteViews
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.rainyseason.cj.R
import com.rainyseason.cj.data.UserCurrency
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TickerWidgerRender @Inject constructor(
    private val context: Context
) {

    @SuppressLint("UnspecifiedImmutableFlag")
    fun render(
        view: RemoteViews,
        userCurrency: UserCurrency,
        config: TickerWidgetConfig,
        data: TickerWidgetDisplayData,
        showLoading: Boolean = false,
        clickToUpdate: Boolean = false,
    ) {
        val renderData = data.addBitmap(context)
        view.setTextViewText(R.id.symbol, renderData.symbol)
        val priceContent = formatPrice(userCurrency, renderData.price)
        view.setTextViewText(R.id.price, priceContent)
        val changes = formatChange(config, renderData)
        view.setTextViewText(R.id.change_percent, changes)
        view.setViewVisibility(R.id.loading, if (showLoading) View.VISIBLE else View.GONE)
        view.setImageViewBitmap(R.id.icon, renderData.iconBitmap)

        if (clickToUpdate) {
            val intent = Intent(context, CoinTickerReceiver::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(config.widgetId))
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                config.widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            view.setOnClickPendingIntent(R.id.container, pendingIntent)
        }
    }

    private fun SpannableStringBuilder.appendChange(amount: Double) {
        val color = if (amount > 0) {
            Color.GREEN
        } else {
            Color.RED
        }
        color(color) {
            val symbol = if (amount > 0) {
                "▴"
            } else {
                "▾"
            }
            append("${symbol}${amount}% ")
        }
    }

    private fun formatChange(
        config: TickerWidgetConfig,
        data: TickerWidgetDisplayData
    ): CharSequence {
        @Suppress("UnnecessaryVariable")
        val content = buildSpannedString {
            if (config.showChange24h) {
                appendChange(data.change24hPercent)
            }
            if (config.showChange7d) {
                appendChange(data.change7dPercent)
            }
            if (config.showChange14d) {
                appendChange(data.change14dPercent)
            }
        }
        return content
    }

    private fun formatPrice(userCurrency: UserCurrency, amount: Double): String {
        @Suppress("UnnecessaryVariable")
        val formatted = if (userCurrency.placeOnTheLeft) {
            "%s%s${amount}".format(userCurrency.symbol, userCurrency.separator)
        } else {
            "${amount}%s%s".format(userCurrency.separator, userCurrency.symbol)
        }
        return formatted
    }
}