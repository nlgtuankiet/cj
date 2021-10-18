package com.rainyseason.cj.coinstat

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.databinding.CoinStatFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

@Module
interface CoinStatFragmentModule {

    @ContributesAndroidInjector
    fun fragment(): CoinStatFragment
}

class CoinStatFragment : Fragment(R.layout.coin_stat_fragment), MavericksView {

    private lateinit var binding: CoinStatFragmentBinding
    private lateinit var navController: NavController

    val args: CoinStatArgs by lazy { requireArgs() }

    @Inject
    lateinit var viewModelFactory: CoinStatViewModel.Factory

    @Inject
    lateinit var controllerFactory: CoinStatController.Factory

    private val viewModel: CoinStatViewModel by fragmentViewModel()
    private val controller: CoinStatController by lazy {
        controllerFactory.create(viewModel, requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = CoinStatFragmentBinding.bind(view)
        navController = binding.root.findNavController()
        binding.backButton.setOnClickListener {
            navController.navigateUp()
        }

        val symbol = args.symbol
        if (symbol != null) {
            binding.title.text = getString(R.string.coin_stat_title, symbol.uppercase())
        }

        binding.epoxyRecyclerView.setController(controller)
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}
