package com.rainyseason.cj.widget.watch

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
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

    private val controller by lazy { controllerFactory.create(viewModel) }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = WatchPreviewFragmentBinding.bind(view)

        binding.settingContent.setController(controller)
        viewModel
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}