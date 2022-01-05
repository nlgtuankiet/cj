package com.rainyseason.cj

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rainyseason.cj.coinstat.CoinStatArgs
import com.rainyseason.cj.coinstat.CoinStatFragment
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.contact.ContactFragment
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.detail.CoinDetailArgs
import com.rainyseason.cj.detail.CoinDetailFragment
import com.rainyseason.cj.detail.about.CoinDetailAboutFragment
import com.rainyseason.cj.setting.SettingFragment
import com.rainyseason.cj.watch.WatchListFragment
import com.rainyseason.cj.widget.manage.ManageWidgetFragment
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Module
interface MainActivityModule {
    @Suppress("unused")
    @ContributesAndroidInjector
    fun activity(): MainActivity
}

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var commonRepository: CommonRepository

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.graph = navController.createGraph(
            R.id.main_nav_graph,
            R.id.manage_widgets_screen,
        ) {
            fragment<ManageWidgetFragment>(R.id.manage_widgets_screen)
            fragment<ContactFragment>(R.id.contact_screen)
            fragment<WatchListFragment>(R.id.watch_list_screen)
            fragment<CoinDetailFragment>(R.id.detail_screen)
            fragment<ReleaseNoteFragment>(R.id.release_note_screen)
            fragment<SettingFragment>(R.id.setting_screen)
            fragment<CoinStatFragment>(R.id.coin_stat_screen)
            fragment<CoinDetailAboutFragment>(R.id.detail_about_screen)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        bottomNav.setOnItemReselectedListener { }
        if (savedInstanceState == null) {
            navigateNewIntent(intent)
        }

        lifecycleScope.launch {
            commonRepository.hasUnreadReleaseNoteFlow()
                .flowOn(Dispatchers.IO)
                .collect {
                    Timber.d("hasUnreadReleaseNoteFlow: $it")
                    val badge = bottomNav.getOrCreateBadge(R.id.release_note_screen)
                    badge.isVisible = it
                }
        }
    }

    private fun navigateNewIntent(intent: Intent) {
        val screen = intent.extras?.getString(SCREEN_TO_OPEN_EXTRA)
        val coinId = intent.extras?.getString(COIN_ID_EXTRA)
            ?: intent.extras?.getString("coinId") // for dev
        val screenId = when (screen) {
            WatchListFragment.SCREEN_NAME -> R.id.watch_list_screen
            CoinDetailFragment.SCREEN_NAME -> R.id.detail_screen
            CoinStatFragment.SCREEN_NAME -> R.id.coin_stat_screen
            else -> null
        }
        Timber.d("onNewIntent: $intent screen = $screen")

        when (screenId) {
            R.id.watch_list_screen -> navController.navigate(screenId)
            R.id.coin_stat_screen -> {
                if (coinId != null) {
                    navController.navigate(
                        R.id.coin_stat_screen,
                        CoinStatArgs(coinId).asArgs()
                    )
                }
            }
            R.id.detail_screen -> {
                if (coinId != null) {
                    navController.navigate(
                        R.id.detail_screen,
                        CoinDetailArgs(coinId).asArgs()
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navigateNewIntent(intent)
    }

    companion object {

        fun watchListIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(SCREEN_TO_OPEN_EXTRA, WatchListFragment.SCREEN_NAME)
            }.maybeNewTask(context)
        }

        fun coinDetailIntent(context: Context, coinId: String): Intent {
            return Intent(context, MainActivity::class.java).apply {
                putExtra(SCREEN_TO_OPEN_EXTRA, CoinDetailFragment.SCREEN_NAME)
                putExtra(COIN_ID_EXTRA, coinId)
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
    }
}
