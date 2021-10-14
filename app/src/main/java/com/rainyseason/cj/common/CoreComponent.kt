package com.rainyseason.cj.common

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.View
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.featureflag.DebugFlagProvider
import com.rainyseason.cj.ticker.CoinTickerHandler
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.widget.watch.WatchWidgetRender
import com.squareup.moshi.Moshi
import okhttp3.Call

interface CoreComponent {
    val callFactory: Call.Factory
    val tickerWidgetRender: TickerWidgetRenderer
    val watchWidgetRenderer: WatchWidgetRender
    val appWidgetManager: AppWidgetManager
    val coinTickerHandler: CoinTickerHandler
    val coinGeckoService: CoinGeckoService
    val moshi: Moshi
    val coinTickerRepository: CoinTickerRepository
    val debugFlagProvider: DebugFlagProvider
    val traceManager: TraceManager
    val tracker: Tracker
    val commonRepository: CommonRepository
    val firebaseCrashlytics: FirebaseCrashlytics
    val numberFormater: NumberFormater
    val graphRenderer: GraphRenderer
}

interface HasCoreComponent {
    val coreComponent: CoreComponent
}

val Context.coreComponent: CoreComponent
    get() = (applicationContext as HasCoreComponent).coreComponent

val View.coreComponent: CoreComponent
    get() = context.coreComponent
