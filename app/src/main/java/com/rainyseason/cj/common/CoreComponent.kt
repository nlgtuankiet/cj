package com.rainyseason.cj.common

import android.content.Context
import okhttp3.Call

interface CoreComponent {
    val callFactory: Call.Factory
}

interface HasCoreComponent {
    val coreComponent: CoreComponent
}

val Context.coreComponent: CoreComponent
    get() = (this as HasCoreComponent).coreComponent