package com.rainyseason.cj.chat.list

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.google.firebase.firestore.FirebaseFirestore
import com.rainyseason.cj.chat.ChatRepository
import com.rainyseason.cj.chat.asFlow
import com.rainyseason.cj.chat.history.MessageEntry
import com.rainyseason.cj.common.fragment
import com.rainyseason.cj.common.update
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map

data class ChatListState(
    val chatList: Async<List<ChatEntry>> = Uninitialized,
    val lastReadMessageId: Map<String, Async<String?>> = emptyMap()
) : MavericksState

data class ChatEntry(
    val id: String,
    val lastMessage: MessageEntry,
)

class ChatListViewModel @AssistedInject constructor(
    @Assisted private val initState: ChatListState,
    private val firebaseFirestore: FirebaseFirestore,
    private val chatRepository: ChatRepository,
) : MavericksViewModel<ChatListState>(initState) {
    private var loadChatIdsJob: Job? = null
    private var onAsyncChatIdsJob: Job? = null
    private val loadLastReadMessageJob = mutableMapOf<String, Job?>()

    init {
        reload()
    }

    fun reload() {
        loadChatIdsJob?.cancel()
        loadChatIdsJob = firebaseFirestore.collection("chats")
            .asFlow()
            .map {
                it.documents.map { document ->
                    ChatEntry(
                        id = document.id,
                        lastMessage = MessageEntry.fromDocumentSnapshot(document)
                    )
                }
            }
            .execute { copy(chatList = it) }

        onAsyncChatIdsJob?.cancel()
        onAsyncChatIdsJob = onAsync(ChatListState::chatList) { chatList ->
            chatList.forEach {
                maybeLoadLastReadMessageId(it)
            }
        }
    }

    private fun maybeLoadLastReadMessageId(chatEntry: ChatEntry) {
        withState { state ->
            val chatId = chatEntry.id
            val currentAsync = state.lastReadMessageId[chatId]
            if (currentAsync == null || currentAsync is Fail) {
                loadLastReadMessageJob[chatId]?.cancel()
                loadLastReadMessageJob[chatId] = chatRepository
                    .lastReadMessageIdFlow(chatId)
                    .execute {
                        copy(
                            lastReadMessageId = lastReadMessageId.update {
                                put(chatId, it)
                            }
                        )
                    }
            }
        }
    }

    companion object : MavericksViewModelFactory<ChatListViewModel, ChatListState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: ChatListState
        ): ChatListViewModel {
            return viewModelContext.fragment<ChatListFragment>().viewModelFactory
                .create(state)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(initState: ChatListState): ChatListViewModel
    }
}
