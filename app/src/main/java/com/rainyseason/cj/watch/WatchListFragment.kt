package com.rainyseason.cj.watch

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.databinding.FragmentWatchListBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

@Module
interface WatchListFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): WatchListFragment
}


class WatchListFragment : Fragment(R.layout.fragment_watch_list), MavericksView {

    @Inject
    lateinit var viewModelFatory: WatchListViewModel.Factory

    @Inject
    lateinit var controllerFactory: WatchListController.Factory

    private val viewModel: WatchListViewModel by fragmentViewModel()

    private val controller: WatchListController by lazy {
        controllerFactory.create(viewModel)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentWatchListBinding.bind(view)
        setupSearchAnimation(binding)
        binding.contentRecyclerView.setController(controller)

    }

    private fun setupSearchAnimation(
        binding: FragmentWatchListBinding,
    ) {
        val searchGroup = binding.searchGroup.searchGroup
        val searchEditText = binding.searchGroup.searchEditText
        binding.searchGroup.cancelSearch.setOnClickListener {
            searchEditText.apply {
                text = null
                clearFocus()
                dismissKeyboard()
            }
        }
        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                searchGroup.transitionToEnd()
            } else {
                searchGroup.transitionToStart()
            }
        }

    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}