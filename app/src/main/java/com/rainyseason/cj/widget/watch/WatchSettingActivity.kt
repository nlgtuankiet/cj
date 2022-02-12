package com.rainyseason.cj.widget.watch

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.rainyseason.cj.R
import com.rainyseason.cj.appendAllMainScreen
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.ticker.getWidgetId
import com.rainyseason.cj.tracking.Tracker
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import timber.log.Timber
import javax.inject.Inject

@Module
interface WatchSettingActivityModule {

    @ContributesAndroidInjector
    fun activity(): WatchSettingActivity
}

class WatchSettingActivity : AppCompatActivity() {

    private var widgetSaved = false
    private var refreshed = false

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
        widgetSaved = intent?.extras?.getBoolean(WIDGET_SAVED_EXTRA, false) == true
        Timber.d("widget id: $widgetId")
        if (widgetId == null) {
            finish()
            return
        }
        setContentView(R.layout.activity_watch_setting)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        navController.graph = navController.createGraph(
            R.id.watchlist_widget_preview_nav_graph,
            R.id.watchlist_widget_preview_screen,
        ) {
            fragment<WatchPreviewFragment>(R.id.watchlist_widget_preview_screen)
            appendAllMainScreen()
        }
    }

    companion object {

        private const val WIDGET_SAVED_EXTRA = "widget_saved"

        fun starterIntent(context: Context, config: WatchConfig): Intent {
            val intent = Intent(context, WatchSettingActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
            intent.putExtra(WIDGET_SAVED_EXTRA, true)
            return intent
        }
    }
}
