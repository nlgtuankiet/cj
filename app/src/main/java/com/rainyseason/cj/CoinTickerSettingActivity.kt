package com.rainyseason.cj

import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity

class CoinTickerSettingActivity : AppCompatActivity() {

    lateinit var remoteView: RemoteViews
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coin_ticker_setting)

        remoteView = LocalRemoteViews(this, R.id.preview_container, R.layout.widget_coin_ticker)
    }
}