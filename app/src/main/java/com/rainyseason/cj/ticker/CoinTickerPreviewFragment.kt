package com.rainyseason.cj.ticker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.rainyseason.cj.R

class CoinTickerPreviewFragment : Fragment(), MavericksView {

    private val viewModel: CoinTickerSettingViewModel by activityViewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.coin_ticker_preview_fragment, container, false)
    }

    override fun invalidate() {

    }
}