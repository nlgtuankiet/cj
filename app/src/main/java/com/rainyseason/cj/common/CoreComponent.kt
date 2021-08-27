package com.rainyseason.cj.common

import android.content.Context
import android.view.View
import com.rainyseason.cj.ticker.TickerWidgerRender
import okhttp3.Call

interface CoreComponent {
    val callFactory: Call.Factory
    val tickerWidgetRender: TickerWidgerRender
}

interface HasCoreComponent {
    val coreComponent: CoreComponent
}

val Context.coreComponent: CoreComponent
    get() = (applicationContext as HasCoreComponent).coreComponent

val View.coreComponent: CoreComponent
    get() = context.coreComponent