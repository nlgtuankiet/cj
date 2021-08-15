package com.rainyseason.cj.ticker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RemoteViews
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.rainyseason.cj.LocalRemoteViews
import com.rainyseason.cj.R

class CoinTickerPreviewFragment : Fragment(), MavericksView {

    private val viewModel: CoinTickerSettingViewModel by activityViewModel()
    private lateinit var remoteView: RemoteViews

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.coin_ticker_preview_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        remoteView = LocalRemoteViews(
            requireContext(),
            view.findViewById(R.id.preview_container),
            R.layout.widget_coin_ticker
        )
        viewModel.onEach {
            updateRemoteView(remoteView, it)
        }

        view.findViewById<Button>(R.id.save_button).setOnClickListener {
            viewModel.save()
        }
    }

    private fun updateRemoteView(view: RemoteViews, state: CoinTickerSettingState) {
        val savedDisplayConfig = state.savedWidgetData.invoke() ?: return
        savedDisplayConfig.bindTo(view)
    }

    override fun invalidate() {

    }
}