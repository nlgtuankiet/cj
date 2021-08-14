package com.rainyseason.cj.ticker

import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.R

class CoinTickerSettingActivity : AppCompatActivity() {

    lateinit var remoteView: RemoteViews
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_coin_ticker_setting)
        println("CoinTickerSettingActivity: onCreate")
        remoteView = LocalRemoteViews(this, R.id.preview_container, R.layout.widget_coin_ticker)
    }
}