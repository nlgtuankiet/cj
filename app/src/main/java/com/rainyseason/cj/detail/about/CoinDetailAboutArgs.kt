package com.rainyseason.cj.detail.about

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CoinDetailAboutArgs(
    val coinName: String,
    val content: String,
) : Parcelable
