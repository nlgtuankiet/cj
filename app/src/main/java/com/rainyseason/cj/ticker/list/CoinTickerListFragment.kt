package com.rainyseason.cj.ticker.list

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.setTextIfDifferent
import com.rainyseason.cj.ticker.CoinTickerNavigator
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import timber.log.Timber
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
        savedInstanceState: Bundle?,
    ): View? {
        Timber.d("onCreateView")
        return inflater.inflate(R.layout.coin_ticker_list_fragment, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("onDestroyView")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val recyclerView = view.findViewById<EpoxyRecyclerView>(R.id.content_recycler_view)
        val backButton = view.findViewById<ImageView>(R.id.back_button)
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }

        val searchBox = view.findViewById<EditText>(R.id.search_box)
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                viewModel.submitNewKeyword(newKeyword = s.toString())
            }
        })

        val clearButton = view.findViewById<ImageView>(R.id.clear_button)
        clearButton.setOnClickListener { viewModel.submitNewKeyword("") }


        viewModel.onEach(CoinTickerListState::keyword) { keyword ->
            searchBox.setTextIfDifferent(keyword)
            clearButton.isVisible = keyword.isNotBlank()
        }


        recyclerView.setController(controller)
        viewModel.onEach {
            controller.requestModelBuild()
        }


    }

    override fun invalidate() {

    }
}