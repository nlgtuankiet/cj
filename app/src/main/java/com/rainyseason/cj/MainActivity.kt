package com.rainyseason.cj

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.createGraph
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.fragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.contact.ContactFragment
import com.rainyseason.cj.common.home.HomeFragment
import com.rainyseason.cj.detail.CoinDetailArgs
import com.rainyseason.cj.detail.CoinDetailFragment
import com.rainyseason.cj.featureflag.FeatureFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.watch.WatchListFragment
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector

@Module
interface MainActivityModule {
    @Suppress("unused")
    @ContributesAndroidInjector
    fun activity(): MainActivity
}

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val enableWatchList = FeatureFlag.WATCH_LIST.isEnable

        navController.graph = navController.createGraph(
            R.id.main_nav_graph,
            if (enableWatchList) {
                R.id.watch_list_screen
            } else {
                R.id.home_screen
            }
        ) {
            fragment<HomeFragment>(R.id.home_screen)
            fragment<ContactFragment>(R.id.contact_screen)
            fragment<WatchListFragment>(R.id.watch_list_screen)
            fragment<CoinDetailFragment>(R.id.detail)
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        if (!enableWatchList) {
            bottomNav.menu.removeItem(R.id.watch_list_screen)
        }
        bottomNav.setupWithNavController(navController)

        if (savedInstanceState == null) {
            val coinId = intent.extras?.getString("coinId")
            if (coinId != null) {
                navController.navigate(R.id.detail, CoinDetailArgs(coinId).asArgs())
            }
        }
    }
}
