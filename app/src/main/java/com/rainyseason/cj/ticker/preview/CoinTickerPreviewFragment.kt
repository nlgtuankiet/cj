package com.rainyseason.cj.ticker.preview

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.stickyheader.StickyHeaderLinearLayoutManager
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.ticker.CoinTickerWidgetSaver
import com.rainyseason.cj.ticker.TickerWidgerRender
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

@Module
interface CoinTickerPreviewFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): CoinTickerPreviewFragment
}

class CoinTickerPreviewFragment : Fragment(), MavericksView {

    private val viewModel: CoinTickerPreviewViewModel by fragmentViewModel()

    @Inject
    lateinit var render: TickerWidgerRender

    @Inject
    lateinit var viewModelFactory: CoinTickerPreviewViewModel.Factory

    private val controller: CoinTickerPreviewController by lazy {
        CoinTickerPreviewController(viewModel, requireContext())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.coin_ticker_preview_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<EpoxyRecyclerView>(R.id.setting_content)
        recyclerView.setController(controller)
        recyclerView.layoutManager = StickyHeaderLinearLayoutManager(requireContext())
        view.findViewById<Button>(R.id.save_button).setOnClickListener {
            save()
        }

        view.findViewById<ImageView>(R.id.back_button).setOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun save() {
        val config = withState(viewModel) { it.savedConfig.invoke() } ?: return
        val displayData = withState(viewModel) { it.savedDisplayData.invoke() } ?: return
        val userCurrency = withState(viewModel) { it.userCurrency.invoke() } ?: return
        (requireActivity() as CoinTickerWidgetSaver).saveWidget(config, displayData, userCurrency)
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}