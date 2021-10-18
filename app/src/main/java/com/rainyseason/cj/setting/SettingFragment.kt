package com.rainyseason.cj.setting

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.databinding.SettingFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

@Module
interface SettingFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): SettingFragment
}

class SettingFragment : Fragment(R.layout.setting_fragment), MavericksView {

    @Inject
    lateinit var viewModelFactory: SettingViewModel.Factory

    @Inject
    lateinit var controllerFactory: SettingController.Factory

    private val viewModel: SettingViewModel by fragmentViewModel()

    private val controller by lazy {
        controllerFactory.create(viewModel, requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    private lateinit var binding: SettingFragmentBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = SettingFragmentBinding.bind(view)
        binding.epoxyRecyclerView.setController(controller)
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}
