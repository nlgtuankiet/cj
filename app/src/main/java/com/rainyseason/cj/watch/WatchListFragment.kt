package com.rainyseason.cj.watch

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.dismissKeyboard
import com.rainyseason.cj.common.setTextIfDifferent
import com.rainyseason.cj.databinding.FragmentWatchListBinding
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logScreenEnter
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
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

    @Inject
    lateinit var tracker: Tracker

    val viewModel: WatchListViewModel by fragmentViewModel()

    val controller: WatchListController by lazy {
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
        setUpEdit(binding)
        tracker.logScreenEnter("watch_list")
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
                searchGroup.transitionToStart()
            }
        }

        searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || !searchEditText.text.isNullOrBlank()) {
                searchGroup.transitionToEnd()
            } else {
                searchGroup.transitionToStart()
            }
        }
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                searchEditText.clearFocus()
                searchEditText.dismissKeyboard()
                return@setOnEditorActionListener true
            }
            false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.onEach(WatchListState::keyword) {
                    searchEditText.setTextIfDifferent(it)
                }
            }
        }

        searchEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onKeywordChange(text?.toString() ?: "")
        }
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}
