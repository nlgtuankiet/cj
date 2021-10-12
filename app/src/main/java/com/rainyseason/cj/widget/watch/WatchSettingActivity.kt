package com.rainyseason.cj.widget.watch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.R
import com.rainyseason.cj.common.ActivityScope
import com.rainyseason.cj.ticker.getWidgetId
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import javax.inject.Inject

@Module
interface WatchSettingActivityModule {

    @ContributesAndroidInjector
    @ActivityScope
    fun activity(): WatchSettingActivity
}

class WatchSettingActivity : AppCompatActivity() {

    private var widgetSaved = false

    @Inject
    lateinit var watchWidgetRepository: WatchWidgetRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        setResult(RESULT_CANCELED)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_setting)
        val widgetId = getWidgetId()
        Timber.d("widget id: $widgetId")
        if (widgetId == null) {
            finish()
            return
        }
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
}