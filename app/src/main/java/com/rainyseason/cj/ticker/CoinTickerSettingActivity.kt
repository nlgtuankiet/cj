package com.rainyseason.cj.ticker

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.viewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.launchAndRepeatWithLifecycle
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@Module
interface CoinTickerSettingActivityModule {
    @ContributesAndroidInjector
    fun activity(): CoinTickerSettingActivity
}

class CoinTickerSettingActivity : AppCompatActivity(), HasAndroidInjector, MavericksView {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var viewModelFactory: CoinTickerSettingViewModel.Factory

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    private val viewModel: CoinTickerSettingViewModel by viewModel()

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

        launchAndRepeatWithLifecycle {
            viewModel.saveEvent.collect {
                save(widgetId = widgetId, data = it)
            }
        }
    }

    private fun save(widgetId: Int, data: TickerWidgetDisplayData) {
        val remoteView = RemoteViews(packageName, R.layout.widget_coin_ticker)
        data.bindTo(remoteView)
        appWidgetManager.updateAppWidget(widgetId, remoteView)
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
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

    override fun invalidate() {

    }
}