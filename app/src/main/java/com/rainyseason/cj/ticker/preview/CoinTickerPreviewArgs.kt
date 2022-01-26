package com.rainyseason.cj.ticker.preview

import android.os.Parcelable
import com.rainyseason.cj.common.model.Backend
import com.rainyseason.cj.ticker.CoinTickerLayout
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinTickerPreviewArgs(
    val widgetId: Int,
    val coinId: String?,
    val layout: CoinTickerLayout,
    val backend: Backend?
) : Parcelable
