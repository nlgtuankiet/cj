package com.rainyseason.cj.common

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMargins
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.rainyseason.cj.R
import com.rainyseason.cj.chat.ChatUtil
import com.rainyseason.cj.chat.history.ChatHistoryArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

private val chatScreenIds = listOf(R.id.chat_history_screen, R.id.chat_login_screen)

@FlowPreview
@OptIn(ExperimentalCoroutinesApi::class)
fun Fragment.setupBottomNav(showChatBadge: Boolean = true) {
    val view = requireView()
    val navController = findNavController()
    val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottom_nav)
    val currentDestinationId = navController.currentDestination?.id ?: return
    if (currentDestinationId in chatScreenIds) {
        bottomNav.selectedItemId = R.id.chat_history_screen
    } else {
        bottomNav.selectedItemId = currentDestinationId
    }
    val coreComponent = requireContext().coreComponent
    val firebaseAuth = coreComponent.firebaseAuth
    bottomNav.setOnItemSelectedListener {
        val currentScreenId = navController.currentDestination?.id ?: 0
        val selectId = it.itemId

        if (selectId in chatScreenIds) {
            if (currentScreenId !in chatScreenIds) {
                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    navController.navigate(R.id.chat_login_screen)
                } else {
                    val chatId = ChatUtil.getChatId(currentUser.uid, ChatUtil.ADMIN_UID)
                    val args = ChatHistoryArgs(chatId)
                    navController.navigate(R.id.chat_history_screen, args.asArgs())
                }
            }
            return@setOnItemSelectedListener false
        }

        if (currentScreenId != it.itemId) {
            navController.navigate(it.itemId)
        }
        false
    }
    bottomNav.setOnItemReselectedListener { }
    viewLifecycleOwner.lifecycleScope.launch {
        // release note badge
        coreComponent.commonRepository.hasUnreadReleaseNoteFlow()
            .flowOn(Dispatchers.IO)
            .collect {
                Timber.d("hasUnreadReleaseNoteFlow: $it")
                val badge = bottomNav.getOrCreateBadge(R.id.release_note_screen)
                badge.isVisible = it
            }

        firebaseAuth.isUserLoginFlow()
            .onEach {
                Timber.d("loggedIn on each: $it")
            }
            .collect {
            }
    }

    viewLifecycleOwner.lifecycleScope.launch {
        if (!showChatBadge) {
            return@launch
        }
        // chat badge (has unread message)
        coreComponent.chatRepository.showChatBadgeFlow()
            .flowOn(Dispatchers.IO)
            .collect {
                val badge = bottomNav.getOrCreateBadge(R.id.chat_history_screen)
                badge.isVisible = it
            }
    }
}

fun Fragment.setupSystemWindows() {
    val view = requireView()
    val bottomNav: View? = view.findViewById<BottomNavigationView>(R.id.bottom_nav)
    if (bottomNav != null) {
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, null)
    }
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(
                top = bars.top,
                right = bars.right,
                bottom = bars.bottom,
                left = bars.left,
            )
        }
        insets
    }
}
