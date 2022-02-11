package com.rainyseason.cj.detail

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.GlideApp
import com.rainyseason.cj.R
import com.rainyseason.cj.common.getDrawableCompat
import com.rainyseason.cj.common.model.Coin
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.common.setupBottomNav
import com.rainyseason.cj.common.setupSystemWindows
import com.rainyseason.cj.databinding.FragmentCoinDetailBinding
import com.rainyseason.cj.tracking.Tracker
import com.rainyseason.cj.tracking.logClick
import com.rainyseason.cj.tracking.logScreenEnter
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

    @Inject
    lateinit var tracker: Tracker

    lateinit var binding: FragmentCoinDetailBinding

    private val viewModel: CoinDetailViewModel by fragmentViewModel()

    private val controller: CoinDetailController by lazy {
        controllerFactory.create(viewModel, args)
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
        bindButton()
        tracker.logScreenEnter(
            SCREEN_NAME,
            mapOf("coin_id" to args.coinId)
        )
        setupBottomNav()
        setupSystemWindows()
    }

    private fun bindButton() {
        val button = binding.addToWatchList
        val startIcon = binding.startIcon
        val loadingDrawable = requireContext()
            .getDrawableCompat(R.drawable.ic_baseline_hourglass_empty_24)
        viewModel.onEach(
            CoinDetailState::defaultWatchListCoins,
            CoinDetailState::addToWatchList,
        ) { watchList, addToWatchList ->
            if (!watchList.complete) {
                button.text = ""
                button.icon = loadingDrawable

                button.setOnClickListener { }

                startIcon.alpha = 0.5f
                startIcon.setOnClickListener { }
            }
            if (watchList.invoke().orEmpty().contains(Coin(args.coinId))) {
                button.setText(R.string.watch_list_menu_remove)
            } else {
                button.setText(R.string.watch_list_menu_add)
            }

            button.setOnClickListener {
                tracker.logClick(
                    screenName = SCREEN_NAME,
                    target = "add_to_watchlist",
                )
                viewModel.onAddToWatchListClick()
            }
            button.icon = if (addToWatchList is Loading) {
                loadingDrawable
            } else {
                null
            }

            startIcon.alpha = if (addToWatchList is Loading) 0.5f else 1f
            if (watchList.invoke().orEmpty().contains(Coin(args.coinId))) {
                startIcon.setImageResource(R.drawable.ic_baseline_star_rate_24)
            } else {
                startIcon.setImageResource(R.drawable.ic_baseline_star_outline_24)
            }
            startIcon.setOnClickListener {
                tracker.logClick(
                    screenName = SCREEN_NAME,
                    target = "star_button"
                )
                viewModel.onAddToWatchListClick()
            }
        }
    }

    private fun bindingHeader() {
        binding.backButton.setOnClickListener {
            tracker.logClick(
                screenName = SCREEN_NAME,
                target = "back_button"
            )
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

    companion object {
        const val SCREEN_NAME = "coin_detail"
    }
}
