package com.rainyseason.cj.ticker

import android.content.Context
import com.bumptech.glide.Glide
import com.rainyseason.cj.R
import timber.log.Timber

@Suppress("UNREACHABLE_CODE")
fun CoinTickerDisplayData.addBitmap(context: Context): CoinTickerDisplayData {
    // temporary disable
    return this
    if (iconBitmap != null) {
        return this
    }
    val size = context.resources.getDimension(R.dimen.widget_coin_ticker_icon_size).toInt()
    val bitmap = Glide.with(context)
        .asBitmap()
        .load(iconUrl)
        .submit(size, size)
        .get()
    Timber.d("add bitmap: ${bitmap.hashCode()}")
    return copy(iconBitmap = bitmap)
}