package com.rainyseason.cj.ticker.preview

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.saveOrShowWarning
import com.rainyseason.cj.databinding.CoinTickerPreviewFragmentBinding
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.CoinTickerWidgetSaver
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.ticker.getWidgetId
import com.rainyseason.cj.tracking.Tracker
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

class CoinTickerPreviewFragment : Fragment(R.layout.coin_ticker_preview_fragment), MavericksView {

    private val viewModel: CoinTickerPreviewViewModel by fragmentViewModel()

    @Inject
    lateinit var render: TickerWidgetRenderer

    @Inject
    lateinit var viewModelFactory: CoinTickerPreviewViewModel.Factory

    @Inject
    lateinit var traceManager: TraceManager

    @Inject
    lateinit var tracker: Tracker

    val args by lazy {
        CoinTickerPreviewArgs(
            widgetId = requireActivity().getWidgetId() ?: error("Missing widget id"),
            coinId = arguments?.getString("coin_id") ?: error("Missing coin id")
        )
    }

    private val controller: CoinTickerPreviewController by lazy {
        CoinTickerPreviewController(viewModel, requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = CoinTickerPreviewFragmentBinding.bind(view)
        val recyclerView = binding.settingContent
        recyclerView.setController(controller)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            save()
        }

        val previewView = binding.previewView
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
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
    }

    private fun save() {
        fun actualSave() {
            viewModel.save()
            val config = withState(viewModel) { it.savedConfig.invoke() } ?: return
            val displayData = withState(viewModel) { it.savedDisplayData.invoke() } ?: return
            (requireActivity() as CoinTickerWidgetSaver).saveWidget(config, displayData)
        }

        val config = withState(viewModel) { it.savedConfig.invoke() } ?: return
        saveOrShowWarning(config.getRefreshMilis()) {
            actualSave()
        }
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}
