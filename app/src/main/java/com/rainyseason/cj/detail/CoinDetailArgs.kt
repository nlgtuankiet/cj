package com.rainyseason.cj.detail

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinDetailArgs(
    val coinId: String,
    val symbol: String? = null,
) : Parcelable
