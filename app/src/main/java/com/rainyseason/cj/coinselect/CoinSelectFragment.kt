package com.rainyseason.cj.coinselect

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.CoinSelectTTI
import com.rainyseason.cj.common.TraceManager
import com.rainyseason.cj.common.setTextIfDifferent
import com.rainyseason.cj.databinding.CoinSelectFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

@Module
interface CoinSelectFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): CoinSelectFragment
}

class CoinSelectFragment : Fragment(R.layout.coin_select_fragment), MavericksView {
    @Inject
    lateinit var viewModelFactory: CoinSelectViewModel.Factory

    @Inject
    lateinit var controllerFactory: CoinSelectController.Factory

    private val viewModel: CoinSelectViewModel by fragmentViewModel()

    @Inject
    lateinit var traceManager: TraceManager

    private val controller: CoinSelectController by lazy {
        controllerFactory.create(viewModel)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            traceManager.beginTrace(CoinSelectTTI(viewModel.id))
            findNavController().addOnDestinationChangedListener(object :
                    NavController.OnDestinationChangedListener {
                    override fun onDestinationChanged(
                        controller: NavController,
                        destination: NavDestination,
                        arguments: Bundle?
                    ) {
                        if (destination.id != R.id.coin_select_screen) {
                            controller.removeOnDestinationChangedListener(this)
                            traceManager.cancelTrace(CoinSelectTTI(viewModel.id))
                        }
                    }
                })
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = CoinSelectFragmentBinding.bind(view)
        setupReview(binding, viewModel.viewModelScope)
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

        viewModel.onEach(CoinSelectState::keyword) { keyword ->
            searchBox.setTextIfDifferent(keyword)
            clearButton.isVisible = keyword.isNotBlank()
        }
        viewModel.onEach(CoinSelectState::backend) { backend ->
            val hintRes = when {
                backend.isDefault -> R.string.search_hint_all
                backend.isExchange -> R.string.search_hint_pair
                else -> R.string.search_hint_coins
            }
            searchBox.setHint(hintRes)
        }

        recyclerView.setController(controller)
        EpoxyVisibilityTracker().attach(recyclerView)

        val onBackPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                viewModel.back()
            }
        }

        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, onBackPressedCallback)

        viewModel.onEach(CoinSelectState::backend) { backend ->
            onBackPressedCallback.isEnabled = !backend.isDefault
        }
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}
