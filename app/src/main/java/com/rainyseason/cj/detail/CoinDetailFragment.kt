package com.rainyseason.cj.detail

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.databinding.FragmentCoinDetailBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject


@Module
interface CoinDetailModule {
    @ContributesAndroidInjector
    fun fragment(): CoinDetailFragment
}

class CoinDetailFragment : Fragment(R.layout.fragment_coin_detail), MavericksView {

    val args: CoinDetailArgs by lazy { requireArgs() }

    @Inject
    lateinit var viewModelFactory: CoinDetailViewModel.Factory

    @Inject
    lateinit var controllerFactory: CoinDetailController.Factory

    lateinit var binding: FragmentCoinDetailBinding

    private val viewModel: CoinDetailViewModel by fragmentViewModel()

    private val controller: CoinDetailController by lazy {
        controllerFactory.create(viewModel)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentCoinDetailBinding.bind(view)
        bindingHeader()
        binding.contentRecyclerView.setController(controller)
    }

    private fun bindingHeader() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.onEach(CoinDetailState::coinDetailResponse) { coinDetailResponse ->
            val response = coinDetailResponse.invoke()
            binding.symbol.text = coinDetailResponse.invoke()?.symbol ?: args.symbol

            val rank = response?.marketCapRank
            binding.rank.text = rank?.let { "#$rank" } ?: ""

            if (response != null) {
                GlideApp.with(binding.coinIcon)
                    .load(response.image.large)
                    .into(binding.coinIcon)
            }
        }
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }

}