package com.rainyseason.cj.widget.watch

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.ticker.getWidgetId
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@Module
interface WatchSettingActivityModule {

    @ContributesAndroidInjector
    @ActivityScope
    fun activity(): WatchSettingActivity
}

class WatchSettingActivity : AppCompatActivity(), WatchWidgetSaver {

    private var widgetSaved = false

    @Inject
    lateinit var watchWidgetRepository: WatchWidgetRepository

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var renderer: WatchWidgetRender

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var watchWidgetHandler: WatchWidgetHandler

    @Inject
    lateinit var commonRepository: CommonRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        setResult(RESULT_CANCELED)
        super.onCreate(savedInstanceState)
        val widgetId = getWidgetId()
        Timber.d("widget id: $widgetId")
        if (widgetId == null) {
            finish()
            return
        }
        setContentView(R.layout.activity_watch_setting)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChangingConfigurations) {
            return
        }
        if (!widgetSaved) {
            runBlocking {
                watchWidgetRepository.clearAllData(getWidgetId() ?: 0)
            }
        }
    }

    override fun saveWidget(config: WatchConfig, data: WatchDisplayData) {
        val param = WatchWidgetRenderParams(
            config = config,
            data = data,
            showLoading = false,
        )

        tracker.logKeyParamsEvent(
            key = "widget_save",
            params = config.getTrackingParams(),
        )

        val remoteView = RemoteViews(packageName, config.layout.layout)
        renderer.render(
            remoteView = remoteView,
            inputParams = param,
        )
        appWidgetManager.updateAppWidget(config.widgetId, remoteView)
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
        }
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                watchWidgetHandler.enqueueRefreshWidget(widgetId = config.widgetId, config = config)
                commonRepository.increaseWidgetUsed()
            }
            widgetSaved = true
            setResult(Activity.RESULT_OK, resultValue)
            finish()
        }
    }
}
