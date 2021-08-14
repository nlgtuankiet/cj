package com.rainyseason.cj

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
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
        GlobalScope.launch {
            withContext(Dispatchers.IO) {
                val response = coinGeckoService.getCoinDetail("dogecoin")
                Timber.d("$response")
            }
        }
    }
}