package com.rainyseason.cj

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
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
        findViewById<Button>(R.id.open_telegram).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("tg://resolve?domain=bwpapp")
            startActivity(intent)
        }
    }



}