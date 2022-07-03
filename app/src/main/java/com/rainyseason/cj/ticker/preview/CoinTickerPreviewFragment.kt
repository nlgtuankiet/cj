package com.rainyseason.cj.ticker.preview

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.R
import com.rainyseason.cj.coinselect.observeCoinSelectResult
import com.rainyseason.cj.common.CoinTickerPreviewTTI
import com.rainyseason.cj.common.OnBoardParam
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.asCoin
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.exception.FailInAppReviewException
import com.rainyseason.cj.common.launchAndRepeatWithViewLifecycle
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.common.saveOrShowWarning
import com.rainyseason.cj.common.show
import com.rainyseason.cj.databinding.CoinTickerPreviewFragmentBinding
import com.rainyseason.cj.featureflag.FeatureFlag
import com.rainyseason.cj.featureflag.isEnable
import com.rainyseason.cj.ticker.CoinTickerRenderParams
import com.rainyseason.cj.ticker.TickerWidgetRenderer
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logKeyParamsEvent
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

    @Inject
    lateinit var appWidgetManager: AppWidgetManager

    @Inject
    lateinit var scope: CoroutineScope

    val args: CoinTickerPreviewArgs by lazy { requireArgs() }

    // todo associate controller with view lifecycle
    private val controller: CoinTickerPreviewController by lazy {
        CoinTickerPreviewController(
            viewModel = viewModel,
            context = requireContext(),
            numberFormater = requireContext().coreComponent.numberFormater,
            renderer = requireContext().coreComponent.tickerWidgetRender
        )
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
        observeCoinSelectResult { result ->
            viewModel.setCoin(result.asCoin())
        }
        viewModel.onEach(CoinTickerPreviewState::widgetSaved) { autoSaved ->
            binding.saveButton.setText(
                if (autoSaved) {
                    R.string.coin_ticker_preview_auto_save_widget
                } else {
                    R.string.coin_ticker_preview_save_widget
                }
            )
        }

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
            viewModel.onBoardFeature.collect { feature ->
                val params = OnBoardParam(
                    coroutineScope = this,
                    focusId = feature.viewId,
                    epoxyRecyclerView = binding.settingContent,
                    controller = controller,
                    parentView = binding.parent,
                    blockerView = binding.blockerView,
                    onboardContainer = binding.onboardContainer,
                    onBoardTitleRes = feature.titleRes,
                    onBoardDescriptionRes = feature.descriptionRes,
                    onBoardImageRes = feature.imageRes,
                    onDoneListener = {
                        viewModel.onBoardFeatureDone(feature)
                    }
                )
                params.show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        traceManager.cancelTrace(CoinTickerPreviewTTI(viewModel.id))
    }

    private fun showInAppReview() {
        if (!FeatureFlag.REQUEST_INAPP_REVIEW.isEnable) {
            return
        }
        val context = requireContext()
        val activity = requireActivity()
        scope.launch(Dispatchers.IO) {
            try {
                tracker.logKeyParamsEvent("request_inapp_review")
                val waitJob = launch {
                    delay(2000)
                }
                val reviewManager = ReviewManagerFactory.create(context)
                val request = reviewManager.requestReview()
                waitJob.join()
                reviewManager.launchReview(activity, request)
            } catch (ex: Exception) {
                context.coreComponent.firebaseCrashlytics.recordException(
                    FailInAppReviewException(ex)
                )
                if (BuildConfig.DEBUG) {
                    ex.printStackTrace()
                }
            }
        }
    }

    private fun save() {
        fun actualSave() {
            val config = withState(viewModel) { it.savedConfig.invoke() } ?: return
            withState(viewModel) { it.savedDisplayData.invoke() } ?: return
            val activity = activity ?: return
            viewModel.save()
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, config.widgetId)
            }
            activity.setResult(Activity.RESULT_OK, resultValue)
            if (config.widgetId > 3) {
                showInAppReview()
            }
            activity.finish()
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
