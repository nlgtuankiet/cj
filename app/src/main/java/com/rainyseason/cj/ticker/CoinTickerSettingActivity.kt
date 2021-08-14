package com.rainyseason.cj.ticker

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.R
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

@Module
interface CoinTickerSettingActivityModule {
    @ContributesAndroidInjector
    fun activity(): CoinTickerSettingActivity
}

class CoinTickerSettingActivity : AppCompatActivity(), HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: CoinTickerSettingViewModel.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidInjection.inject(this)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_coin_ticker_setting)
        val widgetId = getWidgetId()
        if (widgetId == null) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CoinTickerPreviewFragment())
                .commit()
        }
    }


    fun getWidgetId(): Int? {
        val widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            return null
        }
        return widgetId
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }
}