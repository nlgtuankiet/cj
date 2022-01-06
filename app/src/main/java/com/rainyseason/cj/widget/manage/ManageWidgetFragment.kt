package com.rainyseason.cj.widget.manage

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.databinding.ManageWidgetFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

@Module
interface ManageWidgetFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): ManageWidgetFragment
}

class ManageWidgetFragment : Fragment(R.layout.manage_widget_fragment), MavericksView {
    private lateinit var binding: ManageWidgetFragmentBinding

    @Inject
    lateinit var controllerFactory: ManageWidgetController.Factory

    @Inject
    lateinit var viewModelFactory: ManageWidgetViewModel.Factory

    private val viewModel: ManageWidgetViewModel by fragmentViewModel()

    private val controller: ManageWidgetController by lazy {
        controllerFactory.create(viewModel)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ManageWidgetFragmentBinding.bind(view)
        binding.content.setController(controller)
    }

    override fun invalidate() {
        binding.content.requestModelBuild()
    }
}
