package com.rainyseason.cj.watch

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WatchlistArgs(
    val showBottomNav: Boolean = true
) : Parcelable
