package com.rainyseason.cj.widget.watch

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.common.saveOrShowBatteryOptimize
import com.rainyseason.cj.databinding.WatchPreviewFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
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
    lateinit var controllerFactory: WatchController.Factory

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
        binding.backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun save() {
        fun actualSave() {
            viewModel.save()
            val state = withState(viewModel) { it }
            val config = state.savedConfig.invoke() ?: return
            val displayData = state.savedDisplayData.invoke() ?: return
            (requireActivity() as WatchWidgetSaver).saveWidget(config, displayData)
        }

        saveOrShowBatteryOptimize {
            actualSave()
        }
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}

interface WatchWidgetSaver {
    fun saveWidget(
        config: WatchConfig,
        data: WatchDisplayData,
    )
}