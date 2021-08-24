package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector


@Module
interface CoinTickerProviderModule {
    @ContributesAndroidInjector
    fun provider(): CoinTickerProvider
}

class CoinTickerProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        AndroidInjection.inject(this, context)
        super.onReceive(context, intent)
    }
}