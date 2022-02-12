package com.rainyseason.cj.watch

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.setupBottomNav
import com.rainyseason.cj.common.setupSystemWindows
import com.rainyseason.cj.databinding.FragmentWatchListBinding
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logScreenEnter
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

    @Inject
    lateinit var tracker: Tracker

    val args: WatchlistArgs by lazy {
        arguments?.getParcelable("args") ?: WatchlistArgs()
    }

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
        binding.contentRecyclerView.setController(controller)
        setUpEdit(binding, viewModel, controller)
        tracker.logScreenEnter(SCREEN_NAME)
        setupRefreshLayout(binding)
        setupBottomNav()
        if (!args.showBottomNav) {
            binding.bottomNav.isGone = true
        }
        setupSystemWindows()
        setUpAdd(binding, viewModel)
    }

    private fun setupRefreshLayout(binding: FragmentWatchListBinding) {
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            viewModel.reload()
        }
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }

    companion object {
        const val SCREEN_NAME = "watchlist"
    }
}
