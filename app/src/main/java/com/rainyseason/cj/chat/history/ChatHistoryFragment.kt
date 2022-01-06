package com.rainyseason.cj.chat.history

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.airbnb.epoxy.EpoxyVisibilityTracker
import com.airbnb.mvrx.MavericksView
import com.airbnb.mvrx.fragmentViewModel
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.R
import com.rainyseason.cj.chat.ChatUtil
import com.rainyseason.cj.common.requireArgs
import com.rainyseason.cj.common.setupBottomNav
import com.rainyseason.cj.databinding.ChatHistoryFragmentBinding
import dagger.Module
import dagger.android.ContributesAndroidInjector
import dagger.android.support.AndroidSupportInjection
import kotlinx.parcelize.Parcelize
import javax.inject.Inject
import kotlin.math.max

@Module
interface ChatHistoryModule {
    @ContributesAndroidInjector
    fun fragment(): ChatHistoryFragment
}

@Parcelize
data class ChatHistoryArgs(
    val chatId: String,
) : Parcelable

class ChatHistoryFragment : Fragment(R.layout.chat_history_fragment), MavericksView {

    @Inject
    lateinit var viewModelFactory: ChatHistoryViewModel.Factory

    @Inject
    lateinit var controllerFactory: ChatHistoryController.Factory

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    lateinit var binding: ChatHistoryFragmentBinding
    private val visibilityTracker = EpoxyVisibilityTracker()

    val args: ChatHistoryArgs by lazy { requireArgs() }
    private val viewModel: ChatHistoryViewModel by fragmentViewModel()
    private val controller: ChatHistoryController by lazy {
        controllerFactory.create(viewModel, args)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AndroidSupportInjection.inject(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = ChatHistoryFragmentBinding.bind(view)
        binding.sendButton.setOnClickListener {
            binding.content.scrollToPosition(0)
            val text = binding.contentEditText.text?.toString().orEmpty()
            binding.contentEditText.setText("")
            viewModel.sendMessage(text)
        }
        binding.content.setController(controller)
        binding.content.layoutManager = LinearLayoutManager(context)
            .apply {
                reverseLayout = true
            }
        visibilityTracker.attach(binding.content)
        val userId = firebaseAuth.currentUser?.uid ?: ""
        binding.title.text = if (ChatUtil.isAdmin(userId)) {
            ChatUtil.getAnonId(args.chatId).takeLast(4)
        } else {
            getString(R.string.chat_history_title_admin)
        }
        binding.back.setOnClickListener {
            view.findNavController().popBackStack()
        }
        binding.more.setOnClickListener {
            view.findNavController().navigate(R.id.contact_screen)
        }
        setupBottomNav(showChatBadge = false)
        setupMargin(view)
    }

    private fun setupMargin(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav, null)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    top = bars.top,
                    right = bars.right,
                    bottom = bars.bottom,
                    left = bars.left,
                )
            }
            binding.bottomBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                updateMargins(
                    bottom = max(binding.bottomNav.measuredHeight, ime.bottom - bars.bottom)
                )
            }
            insets
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        visibilityTracker.detach(binding.content)
    }

    companion object {
        const val SCREEN_NAME = "chat_history"
    }

    override fun invalidate() {
        controller.requestModelBuild()
    }
}
