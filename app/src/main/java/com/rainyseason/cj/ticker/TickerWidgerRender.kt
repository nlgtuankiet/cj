package com.rainyseason.cj.ticker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.text.SpannableStringBuilder
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.rainyseason.cj.R
import com.rainyseason.cj.common.Theme
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.data.UserCurrency
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class TickerWidgetRenderParams(
    val userCurrency: UserCurrency,
    val config: TickerWidgetConfig,
    val data: TickerWidgetDisplayData,
    val showLoading: Boolean = false,
    val clickToUpdate: Boolean = false,
)

@Singleton
class TickerWidgerRender @Inject constructor(
    private val context: Context
) {

    @LayoutRes
    fun selectLayout(config: TickerWidgetConfig): Int {
        return when (config.extraSize) {
            0 -> R.layout.widget_coin_ticker
            10 -> R.layout.widget_coin_ticker_e10
            20 -> R.layout.widget_coin_ticker_e20
            30 -> R.layout.widget_coin_ticker_e30
            else -> error("not support")
        }
    }

    fun <T> select(theme: String, light: T, dark: T): T {
        if (theme == Theme.LIGHT) {
            return light
        }
        if (theme == Theme.DARK) {
            return dark
        }
        val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (mode == Configuration.UI_MODE_NIGHT_YES) {
            return dark
        }
        return light
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    fun render(
        view: RemoteViews,
        params: TickerWidgetRenderParams,
    ) {
        val theme = params.config.theme
        view.setInt(
            R.id.container,
            "setBackgroundResource",
            select(
                theme,
                R.drawable.coin_ticker_background,
                R.drawable.coin_ticker_background_dark
            )
        )
        val renderData = params.data.addBitmap(context)

        view.setTextViewText(R.id.symbol, renderData.symbol)

        val priceContent = formatPrice(params)
        view.setTextViewText(R.id.price, priceContent)
        view.setTextColor(
            R.id.price,
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )

        val changes = formatChange(params)
        view.setTextViewText(R.id.change_percent, changes)
        view.setViewVisibility(R.id.loading, if (params.showLoading) View.VISIBLE else View.GONE)
        view.setViewVisibility(
            R.id.progress_bar,
            if (params.showLoading) View.VISIBLE else View.GONE
        )
        view.setImageViewBitmap(R.id.icon, renderData.iconBitmap)

        view.setTextViewText(R.id.name, renderData.name)
        view.setTextColor(
            R.id.name,
            select(
                theme,
                context.getColorCompat(R.color.gray_900),
                context.getColorCompat(R.color.gray_50),
            )
        )

        if (params.clickToUpdate) {
            val intent = Intent(context, CoinTickerProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_IDS,
                intArrayOf(params.config.widgetId)
            )
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                params.config.widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            view.setOnClickPendingIntent(R.id.container, pendingIntent)
        }
    }

    private fun SpannableStringBuilder.appendChange(amount: Double, numberOfDecimal: Int?) {
        val color = if (amount > 0) {
            ContextCompat.getColor(context, R.color.green_600)
        } else {
            ContextCompat.getColor(context, R.color.red_600)
        }

        val amountText = if (numberOfDecimal != null) {
            "%.${numberOfDecimal}f".format(amount)
        } else {
            amount.toString()
        }
        color(color) {
            val symbol = if (amount > 0) {
                "+"
            } else {
                ""
            }
            append("${symbol}${amountText}%")
        }
    }

    private fun formatChange(
        params: TickerWidgetRenderParams,
    ): CharSequence {
        val config = params.config
        val data = params.data

        @Suppress("UnnecessaryVariable")
        val content = buildSpannedString {
            val amount = when (config.bottomContentType) {
                BottomContentType.PRICE -> when (config.priceChangeInterval) {
                    ChangeInterval._24H -> data.priceChangePercent24h
                    ChangeInterval._7D -> data.priceChangePercent7d
                    ChangeInterval._14D -> data.priceChangePercent14d
                    ChangeInterval._30D -> data.priceChangePercent30d
                    ChangeInterval._60D -> data.priceChangePercent60d
                    ChangeInterval._1Y -> data.priceChangePercent1y
                    else -> error("unknown ${config.bottomContentType}")
                }
                BottomContentType.MARKET_CAP -> when (config.marketCapChangeInterval) {
                    ChangeInterval._24H -> data.marketCapChangePercent24h
                    else -> error("unknown ${config.marketCapChangeInterval}")
                }
                else -> error("unknown ${config.bottomContentType}")
            }
            appendChange(amount, config.numberOfChangePercentDecimal)
        }
        return content
    }

    private fun formatPrice(
        params: TickerWidgetRenderParams,
    ): String {
        val config = params.config
        val data = params.data
        var amount = when (config.bottomContentType) {
            BottomContentType.PRICE -> data.price
            BottomContentType.MARKET_CAP -> data.marketCap
            else -> error("unknown ${config.bottomContentType}")
        }
        var roundToM = amount >= 1_000_000
        roundToM = false
        if (roundToM) {
            amount = (amount / 1_000_000.0).roundToInt().toDouble()
        }

        val formatter: DecimalFormat = NumberFormat.getCurrencyInstance(Locale.US) as DecimalFormat
        formatter.currency = Currency.getInstance(Locale.US)
        formatter.maximumFractionDigits = config.numberOfPriceDecimal ?: Int.MAX_VALUE
        formatter.minimumFractionDigits = 0
        formatter.isGroupingUsed = config.showThousandsSeparator

        var formattedPrice = formatter.format(amount)
        if (roundToM) {
            formattedPrice += "M"
        }
        return formattedPrice
    }
}