package com.rainyseason.cj.ticker.preview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinTickerPreviewArgs(
    val widgetId: Int,
    val coinId: String,
) : Parcelable