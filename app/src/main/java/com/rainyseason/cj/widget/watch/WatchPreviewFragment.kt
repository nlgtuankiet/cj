package com.rainyseason.cj.widget.watch

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.OnBoardParam
import com.rainyseason.cj.common.launchAndRepeatWithViewLifecycle
import com.rainyseason.cj.common.saveOrShowWarning
import com.rainyseason.cj.common.show
import com.rainyseason.cj.databinding.WatchPreviewFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

@Module
interface WatchPreviewFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): WatchPreviewFragment
}

class WatchPreviewFragment : Fragment(R.layout.watch_preview_fragment), MavericksView {

    @Inject
    lateinit var viewModelFactory: WatchPreviewViewModel.Factory

    @Inject
    lateinit var controllerFactory: WatchPreviewController.Factory

    val viewModel: WatchPreviewViewModel by fragmentViewModel()

    private val controller by lazy { controllerFactory.create(viewModel, requireContext()) }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = WatchPreviewFragmentBinding.bind(view)

        binding.settingContent.setController(controller)
        viewModel.onEach(
            WatchPreviewState::config,
            WatchPreviewState::displayData,
        ) { config, displayData ->
            val params = if (config != null && displayData != null) {
                WatchWidgetRenderParams(
                    config = config,
                    data = displayData,
                    showLoading = false,
                    isPreview = true
                )
            } else {
                null
            }
            binding.previewView.setRenderParams(
                params = params
            )
        }

        var oldScale: Double? = null
        viewModel.onEach(
            WatchPreviewState::config,
            WatchPreviewState::previewScale,
            WatchPreviewState::scalePreview,
        ) { config, previewScale, scalePreview ->
            if (config == null || previewScale == null) {
                return@onEach
            }
            val actualScale = if (scalePreview) {
                previewScale
            } else {
                1.0
            }
            binding.previewView.setScale(actualScale, config)
            if (oldScale != actualScale) {
                oldScale = actualScale
                TransitionManager.beginDelayedTransition(binding.root)
            }
        }

        binding.previewView.setOnScaleClickListener {
            viewModel.switchScalePreview()
        }
        binding.saveButton.setOnClickListener {
            save()
        }

        viewModel.onEach(WatchPreviewState::saved) { saved ->
            binding.saveButton.setText(
                if (saved) {
                    R.string.coin_ticker_preview_auto_save_widget
                } else {
                    R.string.coin_ticker_preview_save_widget
                }
            )
        }
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        launchAndRepeatWithViewLifecycle {
            viewModel.onBoardFeature.collect { feature ->
                OnBoardParam(
                    this,
                    focusId = feature.viewId,
                    epoxyRecyclerView = binding.settingContent,
                    controller = controller,
                    parentView = binding.parent,
                    blockerView = binding.blockerView,
                    onboardContainer = binding.onboardContainer,
                    onBoardTitleRes = R.string.widget_watch_onboard_full_size_title,
                    onBoardDescriptionRes = R.string.widget_watch_onboard_full_size_description,
                    onDoneListener = {
                        viewModel.onOnBoardDone(feature)
                    }
                ).show()
            }
        }
    }

    private fun save() {
        fun actualSave() {
            viewModel.save()
            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, viewModel.args.widgetId)
            }
            requireActivity().setResult(Activity.RESULT_OK, resultValue)
            requireActivity().finish()
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
