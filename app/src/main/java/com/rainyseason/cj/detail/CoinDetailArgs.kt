package com.rainyseason.cj.detail

import android.os.Parcelable
import com.rainyseason.cj.common.model.Coin
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinDetailArgs(
    val coin: Coin,
) : Parcelable
