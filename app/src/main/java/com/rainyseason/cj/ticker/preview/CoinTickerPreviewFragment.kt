package com.rainyseason.cj.ticker.preview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.CoinTickerWidgetSaver
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.ticker.view.CoinTickerPreviewView
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Module
interface CoinTickerPreviewFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): CoinTickerPreviewFragment
}

class CoinTickerPreviewFragment : Fragment(), MavericksView {

    private val viewModel: CoinTickerPreviewViewModel by fragmentViewModel()

    @Inject
    lateinit var render: TickerWidgetRenderer

    @Inject
    lateinit var viewModelFactory: CoinTickerPreviewViewModel.Factory

    private val controller: CoinTickerPreviewController by lazy {
        CoinTickerPreviewController(viewModel, requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.coin_ticker_preview_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<EpoxyRecyclerView>(R.id.setting_content)
        recyclerView.setController(controller)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        view.findViewById<Button>(R.id.save_button).setOnClickListener {
            save()
        }

        view.findViewById<ImageView>(R.id.back_button).setOnClickListener {
            requireActivity().onBackPressed()
        }

        val previewView = view.findViewById<CoinTickerPreviewView>(R.id.preview_view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.stateFlow.map { state ->
                val savedConfig = state.savedConfig.invoke()
                val savedDisplayData = state.savedDisplayData.invoke()
                val params =
                    if (savedConfig != null && savedDisplayData != null) {
                        CoinTickerRenderParams(
                            config = savedConfig,
                            data = savedDisplayData,
                            showLoading = false,
                            isPreview = true,
                        )
                    } else {
                        null
                    }
                params
            }
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect {
                    previewView.setRenderParams(it)
                }
        }
    }

    private fun save() {
        viewModel.save()
        val config = withState(viewModel) { it.savedConfig.invoke() } ?: return
        val displayData = withState(viewModel) { it.savedDisplayData.invoke() } ?: return
        (requireActivity() as CoinTickerWidgetSaver).saveWidget(config, displayData)
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}