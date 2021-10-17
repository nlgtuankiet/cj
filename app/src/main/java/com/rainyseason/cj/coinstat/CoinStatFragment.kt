package com.rainyseason.cj.coinstat

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.rainyseason.cj.R
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.databinding.CoinStatFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
interface CoinStatFragmentModule {

    @ContributesAndroidInjector
    fun fragment(): CoinStatFragment
}

class CoinStatFragment : Fragment(R.layout.coin_stat_fragment) {

    private lateinit var binding: CoinStatFragmentBinding
    private lateinit var navController: NavController

    private val args: CoinStatArgs by lazy { requireArgs() }

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
    }
}