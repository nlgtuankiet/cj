package com.rainyseason.cj.ticker

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.ticker.list.CoinTickerListController
import com.rainyseason.cj.ticker.list.CoinTickerListViewModel
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject
import javax.inject.Provider

@Module
interface CoinTickerListFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): CoinTickerListFragment
}

class CoinTickerListFragment : Fragment(), MavericksView {
    @Inject
    lateinit var viewModelProvider: Provider<CoinTickerListViewModel>

    @Inject
    lateinit var navigator: CoinTickerNavigator

    private val viewModel: CoinTickerListViewModel by fragmentViewModel()

    private val settingViewModel: CoinTickerSettingViewModel by activityViewModel()

    private val controller: CoinTickerListController by lazy {
        CoinTickerListController(
            viewModel = viewModel,
            context = requireContext(),
            navigator = navigator,
        )
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
        return inflater.inflate(R.layout.coin_ticker_list_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<EpoxyRecyclerView>(R.id.content_recycler_view)
        recyclerView.setController(controller)
        viewModel.onEach {
            controller.requestModelBuild()
        }
    }

    override fun invalidate() {

    }
}