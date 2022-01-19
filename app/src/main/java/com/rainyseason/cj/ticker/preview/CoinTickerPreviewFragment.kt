package com.rainyseason.cj.ticker.preview

import android.appwidget.AppWidgetManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.core.view.isGone
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.epoxy.DiffResult
import com.airbnb.epoxy.EpoxyModel
import com.airbnb.epoxy.IdUtils
import com.airbnb.epoxy.OnModelBuildFinishedListener
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.coinselect.CoinSelectResult
import com.rainyseason.cj.common.CoinTickerPreviewTTI
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.hideWithAnimation
import com.rainyseason.cj.common.inflater
import com.rainyseason.cj.common.launchAndRepeatWithViewLifecycle
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.common.saveOrShowWarning
import com.rainyseason.cj.common.showWithAnimation
import com.rainyseason.cj.databinding.CoinTickerPreviewFragmentBinding
import com.rainyseason.cj.databinding.CoinTickerPreviewOnboardCoinSelectBinding
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.CoinTickerWidgetSaver
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.tracking.Tracker
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume

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

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    val args: CoinTickerPreviewArgs by lazy { requireArgs() }

    private val controller: CoinTickerPreviewController by lazy {
        CoinTickerPreviewController(viewModel, requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            traceManager.beginTrace(CoinTickerPreviewTTI(viewModel.id))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = CoinTickerPreviewFragmentBinding.bind(view)
        val recyclerView = binding.settingContent
        recyclerView.setController(controller)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.backButton.setOnClickListener {
            requireActivity().finish()
        }

        binding.saveButton.setOnClickListener {
            save()
        }
        findNavController().currentBackStackEntry?.savedStateHandle?.let { savedStateHandle ->
            savedStateHandle.getLiveData<CoinSelectResult?>("result")
                .observe(viewLifecycleOwner) { result: CoinSelectResult? ->
                    Timber.d("result: $result")
                    if (result == null) {
                        return@observe
                    }
                    viewModel.setCoinId(result.coinId, result.backend)
                    savedStateHandle.set("result", null)
                }
        }
        val autoSaved = args.coinId != null
        binding.saveButton.setText(
            if (autoSaved) {
                R.string.coin_ticker_preview_auto_save_widget
            } else {
                R.string.coin_ticker_preview_save_widget
            }
        )

        val previewView = binding.previewView
        binding.blockerView.setOnClickListener { }
        launchAndRepeatWithViewLifecycle {
            viewModel.stateFlow.map { state ->
                val savedConfig = state.savedConfig.invoke()
                val savedDisplayData = state.savedDisplayData.invoke()
                val params =
                    if (savedConfig != null && savedDisplayData != null) {
                        CoinTickerRenderParams(
                            config = savedConfig,
                            data = savedDisplayData,
                            showLoading = state.currentDisplayData == null,
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
                    if (it != null) {
                        previewView.doOnPreDraw {
                            traceManager.endTrace(CoinTickerPreviewTTI(viewModel.id))
                        }
                    }
                }
        }
        launchAndRepeatWithViewLifecycle {
            viewModel.onBoardCoinSelect.collect {
                showCoinSelectOnBoard(viewLifecycleOwner.lifecycleScope, binding)
            }
        }
    }

    private suspend fun RecyclerView.awaitScrollFinish() {
        yield()
        if (scrollState == RecyclerView.SCROLL_STATE_IDLE) {
            return
        }
        suspendCancellableCoroutine<Unit> { cont ->
            val listener = object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        removeOnScrollListener(this)
                        cont.resume(Unit)
                    }
                }
            }
            cont.invokeOnCancellation {
                removeOnScrollListener(listener)
            }
            addOnScrollListener(listener)
        }
    }

    private suspend fun showCoinSelectOnBoard(
        scope: CoroutineScope,
        binding: CoinTickerPreviewFragmentBinding
    ) {
        val adapter = controller.adapter
        val recyclerView = binding.settingContent
        var index = -1

        fun findIndex(models: List<EpoxyModel<*>>) {
            Timber.d("find index")
            index = models.indexOfFirst {
                it.id() == IdUtils.hashString64Bit(CoinTickerPreviewController.COIN_SELECT_ID)
            }
        }

        fun cleanUp() {
            binding.blockerView.isGone = true
            binding.onboardContainer.isGone = true
            binding.onboardContainer.removeAllViews()
        }
        findIndex(adapter.copyOfModels)
        if (index == -1) {
            Timber.d("model is empty")
            suspendCancellableCoroutine<Unit> { cont ->
                val listener = object : OnModelBuildFinishedListener {
                    override fun onModelBuildFinished(result: DiffResult) {
                        findIndex(adapter.copyOfModels)
                        if (index != -1) {
                            cont.resume(Unit)
                            controller.removeModelBuildListener(this)
                        }
                    }
                }
                cont.invokeOnCancellation {
                    controller.removeModelBuildListener(listener)
                }
                controller.addModelBuildListener(listener)
            }
        }

        binding.blockerView.isGone = false
        binding.onboardContainer.isGone = false
        recyclerView.smoothScrollToPosition(index)
        recyclerView.awaitScrollFinish()
        yield()
        val viewHolder = recyclerView.findViewHolderForAdapterPosition(index)
        if (viewHolder == null) {
            cleanUp()
            return
        }
        val itemView = viewHolder.itemView
        val focusHeight = itemView.measuredHeight
        val focusMarginTop = binding.previewView.measuredHeight
        val focusBinding = CoinTickerPreviewOnboardCoinSelectBinding.inflate(
            binding.root.inflater,
            binding.onboardContainer,
            true
        )
        focusBinding.focusPoint.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            height = focusHeight
            updateMargins(top = focusMarginTop)
        }
        binding.onboardContainer.showWithAnimation()
        focusBinding.ok.setOnClickListener {
            scope.launch {
                viewModel.onBoardCoinSelectDone()
                binding.onboardContainer.hideWithAnimation()
                cleanUp()
            }
        }
        Timber.d("done")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        traceManager.cancelTrace(CoinTickerPreviewTTI(viewModel.id))
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
