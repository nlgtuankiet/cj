package com.rainyseason.cj

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.createGraph
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import com.rainyseason.cj.chat.history.ChatHistoryArgs
import com.rainyseason.cj.chat.history.ChatHistoryFragment
import com.rainyseason.cj.chat.list.ChatListFragment
import com.rainyseason.cj.chat.login.ChatLoginFragment
import com.rainyseason.cj.coinselect.CoinSelectFragment
import com.rainyseason.cj.coinstat.CoinStatArgs
import com.rainyseason.cj.coinstat.CoinStatFragment
import com.rainyseason.cj.common.ConfigManager
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.contact.ContactFragment
import com.rainyseason.cj.common.home.AddWidgetTutorialFragment
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.notNull
import com.rainyseason.cj.common.widgetId
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.detail.CoinDetailArgs
import com.rainyseason.cj.detail.CoinDetailFragment
import com.rainyseason.cj.detail.about.CoinDetailAboutFragment
import com.rainyseason.cj.setting.SettingFragment
import com.rainyseason.cj.ticker.CoinTickerHandler
import com.rainyseason.cj.watch.WatchListFragment
import com.rainyseason.cj.widget.manage.ManageWidgetFragment
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Module
interface MainActivityModule {
    @Suppress("unused")
    @ContributesAndroidInjector
    fun activity(): MainActivity
}

fun NavGraphBuilder.appendAllMainScreen() {
    fragment<ChatHistoryFragment>(R.id.chat_history_screen)
    fragment<ChatListFragment>(R.id.chat_list_screen)
    fragment<ChatLoginFragment>(R.id.chat_login_screen)
    fragment<ManageWidgetFragment>(R.id.manage_widgets_screen)
    fragment<ContactFragment>(R.id.contact_screen)
    fragment<WatchListFragment>(R.id.watch_list_screen)
    fragment<CoinDetailFragment>(R.id.detail_screen)
    fragment<ReleaseNoteFragment>(R.id.release_note_screen)
    fragment<SettingFragment>(R.id.setting_screen)
    fragment<CoinStatFragment>(R.id.coin_stat_screen)
    fragment<CoinSelectFragment>(R.id.coin_select_screen)
    fragment<CoinDetailAboutFragment>(R.id.detail_about_screen)
    fragment<AddWidgetTutorialFragment>(R.id.add_widget_tutorial_screen)
}

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var commonRepository: CommonRepository

    @Inject
    lateinit var coinTickerHandler: CoinTickerHandler

    @Inject
    lateinit var configManager: ConfigManager

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        if (BuildConfig.DEBUG) {
            navController.addOnDestinationChangedListener { _, destination, arguments ->
                val className = Class.forName(
                    (destination as FragmentNavigator.Destination).className
                ).simpleName
                Timber.d("Navigation: destination $className args: $arguments")
            }
        }
        checkWidgetId(intent)
        navController.graph = navController.createGraph(
            R.id.main_nav_graph,
            R.id.manage_widgets_screen,
        ) {
            appendAllMainScreen()
        }
        if (savedInstanceState == null) {
            navigateNewIntent(intent)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    private fun navigateNewIntent(intent: Intent) {
        val screen = intent.extras?.getString(SCREEN_TO_OPEN_EXTRA)
        val coinId = intent.extras?.getString(COIN_ID_EXTRA)
            ?: intent.extras?.getString("coinId") // for dev
        val coin = intent.extras?.getParcelable<Coin>("coin")
        val screenId = when (screen) {
            WatchListFragment.SCREEN_NAME -> R.id.watch_list_screen
            CoinDetailFragment.SCREEN_NAME -> R.id.detail_screen
            CoinStatFragment.SCREEN_NAME -> R.id.coin_stat_screen
            ChatHistoryFragment.SCREEN_NAME -> R.id.chat_history_screen
            ChatListFragment.SCREEN_NAME -> R.id.chat_list_screen
            CoinSelectFragment.SCREEN_NAME -> R.id.coin_select_screen
            else -> null
        }
        Timber.d("onNewIntent: $intent screen = $screen")

        when (screenId) {
            R.id.chat_history_screen -> {
                val chatId = intent.extras?.getString("chat_id").notNull()
                val args = ChatHistoryArgs(chatId)
                navController.navigate(screenId, args.asArgs())
            }
            R.id.chat_list_screen -> {
                navController.navigate(screenId)
            }
            R.id.watch_list_screen -> navController.navigate(screenId)
            R.id.coin_stat_screen -> {
                if (coinId != null) {
                    navController.navigate(
                        R.id.coin_stat_screen,
                        CoinStatArgs(coinId).asArgs()
                    )
                }
            }
            R.id.coin_select_screen -> {
                navController.navigate(R.id.coin_select_screen)
            }
            R.id.detail_screen -> {
                if (coin != null) {
                    navController.navigate(
                        R.id.detail_screen,
                        CoinDetailArgs(coin).asArgs()
                    )
                }
            }
        }
    }

    private fun checkWidgetId(intent: Intent?) {
        val widgetId = intent?.widgetId() ?: return
        lifecycleScope.launch {
            coinTickerHandler.cleanUpIfWidgetDeleted(widgetId)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateNewIntent(intent)
        checkWidgetId(intent)
    }

    companion object {

        fun watchListIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(SCREEN_TO_OPEN_EXTRA, WatchListFragment.SCREEN_NAME)
            }.maybeNewTask(context)
        }

        fun coinDetailIntent(context: Context, coin: Coin): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(SCREEN_TO_OPEN_EXTRA, CoinDetailFragment.SCREEN_NAME)
                putExtra(COIN_EXTRA, coin)
            }.maybeNewTask(context)
        }

        fun chatListIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(SCREEN_TO_OPEN_EXTRA, ChatListFragment.SCREEN_NAME)
            }.maybeNewTask(context)
        }

        private fun Intent.maybeNewTask(context: Context): Intent {
            if (context is Activity) {
                return this
            }
            return addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        private const val SCREEN_TO_OPEN_EXTRA = "screen"
        private const val COIN_ID_EXTRA = "coin_id"
        private const val COIN_EXTRA = "coin"
    }
}
