package com.rainyseason.cj.ticker

import android.widget.RemoteViews
import com.rainyseason.cj.R

fun TickerWidgetDisplayConfig.render(view: RemoteViews) {
    val config = this
    view.setTextViewText(R.id.symbol, config.symbol)
    view.setTextViewText(R.id.price, config.currentPrice.toString())
}