package com.rainyseason.cj.chat.list

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.rainyseason.cj.R
import com.rainyseason.cj.common.setupSystemWindows
import com.rainyseason.cj.databinding.ChatListFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import javax.inject.Inject

@Module
interface ChatListModule {
    @ContributesAndroidInjector
    fun fragment(): ChatListFragment
}

class ChatListFragment : Fragment(R.layout.chat_list_fragment), MavericksView {

    @Inject
    lateinit var viewModelFactory: ChatListViewModel.Factory

    @Inject
    lateinit var controllerFactory: ChatListController.Factory

    private val viewModel: ChatListViewModel by fragmentViewModel()

    private lateinit var controller: ChatListController

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller = controllerFactory.create(viewModel)
        val binding = ChatListFragmentBinding.bind(view)
        binding.content.setController(controller)
        setupSystemWindows()
        binding.back.setOnClickListener {
            view.findNavController().popBackStack()
        }
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }

    companion object {
        const val SCREEN_NAME = "chat_list"
    }
}
