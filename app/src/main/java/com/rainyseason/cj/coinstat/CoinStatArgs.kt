package com.rainyseason.cj.coinstat

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinStatArgs(
    val coinId: String,
    val symbol: String? = null
): Parcelable