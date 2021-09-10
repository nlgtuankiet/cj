package com.rainyseason.cj.ticker

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.data.UserCurrency
import com.rainyseason.cj.data.local.CoinTickerRepository
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
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
        config: CoinTickerConfig,
        data: CoinTickerDisplayData
    )
}

class CoinTickerSettingActivity : AppCompatActivity(), HasAndroidInjector,
    CoinTickerWidgetSaver {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var render: TickerWidgerRender

    @Inject
    lateinit var coinTickerHandler: CoinTickerHandler

    @Inject
    lateinit var coinTickerRepository: CoinTickerRepository

    @Inject
    lateinit var navigator: CoinTickerNavigator

    private var widgetSaved = false

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

        val coinId = intent.extras?.getString(COIN_ID_EXTRA)
        if (coinId != null) {
            navigator.moveToPreview(coinId)
        }

        if (BuildConfig.DEBUG) {
            lifecycleScope.launch {
                val ids = coinTickerRepository.getAllDataIds()
                Timber.d("data widget ids: $ids")
            }
        }
    }

    override fun saveWidget(
        userCurrency: UserCurrency,
        config: CoinTickerConfig,
        data: CoinTickerDisplayData
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
            widgetSaved = true
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChangingConfigurations) {
            return
        }
        if (!widgetSaved) {
            runBlocking {
                coinTickerRepository.clearAllData(getWidgetId() ?: 0)
            }
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


    companion object {
        const val COIN_ID_EXTRA = "coin_id"
    }

}