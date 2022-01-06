package com.rainyseason.cj.chat.login

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.common.fragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.tasks.await

data class ChatLoginState(
    val loginTask: Async<Unit> = Uninitialized
) : MavericksState

class ChatLoginViewModel @AssistedInject constructor(
    @Assisted private val initState: ChatLoginState,
    private val firebaseAuth: FirebaseAuth,
) : MavericksViewModel<ChatLoginState>(initState) {

    private var loginJob: Job? = null

    private val _navigateToChatEvent = Channel<Unit>(capacity = Channel.CONFLATED)
    val navigateToChatEvent = _navigateToChatEvent.receiveAsFlow()

    init {
        load()
    }

    fun load() {
        loginJob?.cancel()
        loginJob = suspend {
            login()
        }.execute { copy(loginTask = it) }
    }

    private suspend fun login() {
        if (firebaseAuth.currentUser != null) {
            return
        }
        firebaseAuth.signInAnonymously().await()
        if (BuildConfig.DEBUG) {
            delay(3000)
        }
        _navigateToChatEvent.trySend(Unit)
    }

    companion object : MavericksViewModelFactory<ChatLoginViewModel, ChatLoginState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: ChatLoginState
        ): ChatLoginViewModel {
            return viewModelContext.fragment<ChatLoginFragment>().viewModelFactory
                .create(state)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initState: ChatLoginState): ChatLoginViewModel
    }
}
