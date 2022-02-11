package com.rainyseason.cj.watch

import androidx.navigation.fragment.findNavController
import com.rainyseason.cj.R
import com.rainyseason.cj.coinselect.observeCoinSelectResult
import com.rainyseason.cj.common.asCoin
import com.rainyseason.cj.databinding.FragmentWatchListBinding

fun WatchListFragment.setUpAdd(
    binding: FragmentWatchListBinding,
    viewModel: WatchListViewModel,
) {
    observeCoinSelectResult { result ->
        viewModel.addOrRemove(result.asCoin())
    }

    binding.addButton.setOnClickListener {
        findNavController().navigate(R.id.coin_select_screen)
    }
}
