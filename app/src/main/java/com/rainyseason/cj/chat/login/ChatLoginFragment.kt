package com.rainyseason.cj.chat.login

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.R
import com.rainyseason.cj.chat.ChatUtil
import com.rainyseason.cj.chat.history.ChatHistoryArgs
import com.rainyseason.cj.common.asArgs
import com.rainyseason.cj.common.setupBottomNav
import com.rainyseason.cj.common.setupSystemWindows
import com.rainyseason.cj.databinding.ChatLoginFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@Module
interface ChatLoginModule {
    @ContributesAndroidInjector
    fun fragment(): ChatLoginFragment
}

class ChatLoginFragment : Fragment(R.layout.chat_login_fragment), MavericksView {

    @Inject
    lateinit var viewModelFactory: ChatLoginViewModel.Factory

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private val viewModel: ChatLoginViewModel by fragmentViewModel()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = ChatLoginFragmentBinding.bind(view)
        val loadingGroup = listOf(binding.progressBar, binding.title)
        val retryGroup = listOf(binding.retry)
        viewModel.onEach(ChatLoginState::loginTask) { loginTask ->
            loadingGroup.forEach {
                it.isVisible = loginTask is Loading
            }
            retryGroup.forEach {
                it.isVisible = loginTask is Fail
            }
        }
        binding.retry.setOnClickListener {
            viewModel.load()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.navigateToChatEvent.collect {
                    val currentUser = firebaseAuth.currentUser ?: return@collect
                    val chatId = ChatUtil.getChatId(currentUser.uid, ChatUtil.ADMIN_UID)
                    val args = ChatHistoryArgs(chatId = chatId)

                    findNavController().run {
                        popBackStack()
                        navigate(
                            R.id.chat_history_screen,
                            args.asArgs()
                        )
                    }
                }
            }
        }

        setupBottomNav()
        setupSystemWindows()
    }

    override fun invalidate() {
    }
}
