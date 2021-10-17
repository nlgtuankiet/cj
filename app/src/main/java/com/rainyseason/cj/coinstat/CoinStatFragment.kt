package com.rainyseason.cj.coinstat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rainyseason.cj.R
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = CoinStatFragmentBinding.bind(view)
    }
}