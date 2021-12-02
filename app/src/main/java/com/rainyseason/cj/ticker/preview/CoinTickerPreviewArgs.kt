package com.rainyseason.cj.ticker.preview

import android.os.Parcelable
import com.rainyseason.cj.common.model.Exchange
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinTickerPreviewArgs(
    val widgetId: Int,
    val coinId: String,
    val layout: String,
    val exchange: Exchange?
) : Parcelable
