package com.rainyseason.cj

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.getColorCompat
import com.rainyseason.cj.data.coingecko.CoinGeckoService
import dagger.Module
import dagger.android.AndroidInjection
import dagger.android.ContributesAndroidInjector
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
        findViewById<Button>(R.id.open_telegram).setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("tg://resolve?domain=bwpapp")
                startActivity(intent)
            } catch (ex: ActivityNotFoundException) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://t.me/bwpapp")
                startActivity(intent)
            }
        }
        window.statusBarColor = getColorCompat(R.color.gray_900)
    }


    @Suppress("unused")
    @DelicateCoroutinesApi
    private fun generateCoinList() {
        GlobalScope.launch(Dispatchers.IO) {
            val builder = StringBuilder()
            coreComponent.coinGeckoService.getCoinMarkets("usd", 1000)
                .forEach {
                    val symbol = it.symbol.uppercase()
                    builder.append("$symbol ")
                    val name = it.name
                    builder.append("$name ")
                }
            println("coin list: $builder")
        }
    }


}