package com.rainyseason.cj.ticker

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.airbnb.mvrx.MavericksView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.data.UserCurrency
import com.rainyseason.cj.ticker.list.CoinTickerListFragment
import com.rainyseason.cj.ticker.list.CoinTickerListFragmentModule
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewFragmentModule
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Module
interface CoinTickerSettingActivityModule {
    @ContributesAndroidInjector(
        modules = [
            CoinTickerPreviewFragmentModule::class,
            CoinTickerListFragmentModule::class,
        ]
    )
    @ActivityScope
    fun activity(): CoinTickerSettingActivity
}

interface CoinTickerWidgetSaver {
    fun saveWidget(
        userCurrency: UserCurrency,
        config: TickerWidgetConfig,
        data: TickerWidgetDisplayData
    )
}

class CoinTickerSettingActivity : AppCompatActivity(), HasAndroidInjector, MavericksView,
    CoinTickerWidgetSaver {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var render: TickerWidgerRender

    @Inject
    lateinit var coinTickerHandler: CoinTickerHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_coin_ticker_setting)
        val widgetId = getWidgetId()
        if (widgetId == null) {
            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, CoinTickerListFragment())
                .commit()
        }
    }

    override fun saveWidget(
        userCurrency: UserCurrency,
        config: TickerWidgetConfig,
        data: TickerWidgetDisplayData
    ) {
        val param = TickerWidgetRenderParams(
            userCurrency = userCurrency,
            config = config,
            data = data,
            showLoading = false,
            clickToUpdate = true,
        )
        val remoteView = RemoteViews(packageName, render.selectLayout(config))
        render.render(
            view = remoteView,
            params = param,
        )
        appWidgetManager.updateAppWidget(config.widgetId, remoteView)
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                coinTickerHandler.enqueueRefreshWidget(widgetId = config.widgetId, config = config)
            }
            setResult(Activity.RESULT_OK, resultValue)
            finish()
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

    override fun invalidate() {

    }


}