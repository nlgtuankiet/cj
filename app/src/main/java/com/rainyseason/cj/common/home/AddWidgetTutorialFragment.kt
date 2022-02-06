package com.rainyseason.cj.common.home

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.rainyseason.cj.R
import com.rainyseason.cj.common.buildModels
import com.rainyseason.cj.common.coreComponent
import com.rainyseason.cj.common.setupSystemWindows
import com.rainyseason.cj.data.CommonRepository
import com.rainyseason.cj.databinding.FragmentAddWidgetTutorialBinding
import com.rainyseason.cj.tracking.logScreenEnter
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Module
interface AddWidgetTutorialFragmentModule {
    @ContributesAndroidInjector
    fun fragment(): AddWidgetTutorialFragment
}

class AddWidgetTutorialFragment : Fragment(R.layout.fragment_add_widget_tutorial) {

    @Inject
    lateinit var commonRepository: CommonRepository

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAddWidgetTutorialBinding.bind(view)
        val models = buildModels {
            listOf(
                R.drawable.step_1,
                R.drawable.step_2,
                R.drawable.step_3,
            ).forEach {
                demoView {
                    id(it)
                    imageRes(it)
                }
            }
        }

        binding.carousel.layoutManager = LinearLayoutManager(
            requireContext(),
            RecyclerView.HORIZONTAL,
            false
        )
        val snapHelper = PagerSnapHelper()

        binding.carousel.setModels(models)
        binding.carousel.apply {
            clearOnScrollListeners()
            onFlingListener = null
        }
        snapHelper.attachToRecyclerView(binding.carousel)

        binding.indicator.attachToRecyclerView(binding.carousel, snapHelper)
        binding.doneButton.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    commonRepository.setDoneShowAddWidgetTutorial()
                }
            }
            view.findNavController().popBackStack()
        }

        requireContext().coreComponent.tracker
            .logScreenEnter("tutorial")
        setupSystemWindows()
    }
}
