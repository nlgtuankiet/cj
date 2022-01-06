package com.rainyseason.cj.coinselect

import android.os.Parcelable
import com.rainyseason.cj.common.model.Backend
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinSelectResult(
    val coinId: String,
    val backend: Backend,
) : Parcelable
