package com.rainyseason.cj.widget.manage

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.setupBottomNav
import com.rainyseason.cj.common.setupSystemWindows
import com.rainyseason.cj.databinding.ManageWidgetFragmentBinding
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logClick
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

    @Inject
    lateinit var tracker: Tracker

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
        setupBottomNav()
        setupSystemWindows()
        binding.helpButton.setOnClickListener {
            tracker.logClick(
                screenName = SCREEN_NAME,
                target = "help_button"
            )
            findNavController().navigate(R.id.add_widget_tutorial_screen)
        }
    }

    override fun invalidate() {
        binding.content.requestModelBuild()
    }

    companion object {
        const val SCREEN_NAME = "manage_widget"
    }
}
