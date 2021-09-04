package com.rainyseason.cj

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import javax.inject.Inject

@Module
interface MainActivityModule {
    @ContributesAndroidInjector
    fun activity(): MainActivity
}

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var coinGeckoService: CoinGeckoService

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}