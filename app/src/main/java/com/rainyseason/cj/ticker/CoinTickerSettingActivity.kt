package com.rainyseason.cj.ticker

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.createGraph
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.fragment
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.coinselect.CoinSelectFragment
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.widgetId
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.data.local.CoinTickerRepository
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewArgs
import com.rainyseason.cj.ticker.preview.CoinTickerPreviewFragment
import com.rainyseason.cj.tracking.Tracker
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.ContributesAndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Module
interface CoinTickerSettingActivityModule {
    @ContributesAndroidInjector
    fun activity(): CoinTickerSettingActivity
}

class CoinTickerSettingActivity :
    AppCompatActivity(),
    HasAndroidInjector {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var render: TickerWidgetRenderer

    @Inject
    lateinit var coinTickerHandler: CoinTickerHandler

    @Inject
    lateinit var coinTickerRepository: CoinTickerRepository

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var traceManager: TraceManager

    @Inject
    lateinit var commonRepository: CommonRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        val widgetId = getWidgetId()
        if (widgetId == null) {
            finish()
            return
        }

        val resultIntent = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_CANCELED, resultIntent)
        if (!BuildConfig.DEBUG) {
            lifecycleScope.launch {
                val deleted = coinTickerHandler.cleanUpIfWidgetDeleted(widgetId)
                if (deleted) {
                    finish()
                }
            }
        }

        setContentView(R.layout.activity_coin_ticker_setting)
        Timber.d("widgetId: $widgetId")
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)!!
        val navController = navHostFragment.findNavController()
        val graph = navController.createGraph(
            R.id.coin_ticker_nav_graph,
            R.id.coin_ticker_preview_screen,
        ) {
            fragment<CoinSelectFragment>(R.id.coin_select_screen)
            fragment<CoinTickerPreviewFragment>(R.id.coin_ticker_preview_screen)
        }
        Timber.d("Calling activity: $callingActivity")
        val args = CoinTickerPreviewArgs(
            widgetId = widgetId,
            callingComponent = callingActivity,
        ).asArgs()

        navController.setGraph(
            graph,
            args,
        )
        logDisplay()
    }

    private fun logDisplay() {
        if (!BuildConfig.DEBUG) {
            return
        }
        val display: Display = windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = resources.displayMetrics.density
        val dpHeight = outMetrics.heightPixels / density
        val dpWidth = outMetrics.widthPixels / density
        Timber.d("Screen size dp: ${dpWidth}x$dpHeight")
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return androidInjector
    }

    companion object {
        fun starterIntent(
            context: Context,
            widgetId: Int,
        ): Intent {
            val intent = Intent(context, CoinTickerSettingActivity::class.java)
            intent.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                widgetId,
            )
            return intent
        }
    }
}

fun Activity.getWidgetId(): Int? {
    return intent?.widgetId()
}
