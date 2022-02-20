package com.rainyseason.cj.ticker.preview

import android.content.ComponentName
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinTickerPreviewArgs(
    val widgetId: Int,
    val callingComponent: ComponentName? = null
) : Parcelable
