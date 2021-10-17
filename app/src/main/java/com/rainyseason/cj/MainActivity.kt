package com.rainyseason.cj

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.createGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.contact.ContactFragment
import com.rainyseason.cj.common.home.HomeFragment
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.detail.CoinDetailArgs
import com.rainyseason.cj.detail.CoinDetailFragment
import com.rainyseason.cj.setting.SettingFragment
import com.rainyseason.cj.watch.WatchListFragment
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

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        navController.graph = navController.createGraph(
            R.id.main_nav_graph,
            R.id.watch_list_screen,
        ) {
            fragment<HomeFragment>(R.id.home_screen)
            fragment<ContactFragment>(R.id.contact_screen)
            fragment<WatchListFragment>(R.id.watch_list_screen)
            fragment<CoinDetailFragment>(R.id.detail)
            fragment<ReleaseNoteFragment>(R.id.release_note_screen)
            fragment<SettingFragment>(R.id.setting_screen)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        if (savedInstanceState == null) {
            val coinId = intent.extras?.getString("coinId")
            if (coinId != null) {
                navController.navigate(R.id.detail, CoinDetailArgs(coinId).asArgs())
            }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent: $intent")
        val screenId = intent.extras?.getInt(SCREEN_TO_OPEN_EXTRA)
        if (screenId != null) {
            findNavController(R.id.nav_host_fragment)
                .navigate(screenId)
        }
    }

    companion object {
        const val SCREEN_TO_OPEN_EXTRA = "SCREEN_TO_OPEN"
    }
}
