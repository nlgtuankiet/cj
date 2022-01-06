package com.rainyseason.cj.chat.history

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.Uninitialized
import com.airbnb.mvrx.ViewModelContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.chat.ChatRepository
import com.rainyseason.cj.chat.ChatUtil
import com.rainyseason.cj.chat.asFlow
import com.rainyseason.cj.common.fragment
import com.rainyseason.cj.common.notNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.threeten.bp.Instant
import timber.log.Timber

data class ChatHistoryState(
    val id: Int = 0,
    val messages: Async<List<MessageEntry>> = Uninitialized,
    val loadMessagesTask: Async<Unit> = Uninitialized,
    val sendMessageTask: Async<Unit> = Uninitialized,
) : MavericksState

class ChatHistoryViewModel @AssistedInject constructor(
    @Assisted initState: ChatHistoryState,
    @Assisted private val args: ChatHistoryArgs,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
    private val chatRepository: ChatRepository,
) : MavericksViewModel<ChatHistoryState>(initState) {
    private val maxMessage = 25L

    init {
        reload()
        if (BuildConfig.DEBUG) {
            onEach { state ->
                listOf(
                    state.loadMessagesTask,
                    state.sendMessageTask,
                    state.messages,
                )
                    .forEach {
                        if (it is Fail) {
                            throw it.error
                        }
                    }
            }
        }
    }

    private fun reload() {
        suspend { loadMessages() }
            .execute { copy(loadMessagesTask = it) }
    }

    private suspend fun ensureSignInAnonymously(): FirebaseUser {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            firebaseAuth.signInAnonymously().await()
        }
        return firebaseAuth.currentUser.notNull()
    }

    private var loadMessageJob: Job? = null

    private suspend fun loadMessages() {
        val user = ensureSignInAnonymously()
        Timber.d("user is ${user.displayName} isAnonymous ${user.isAnonymous} ${user.uid}")
        val chatId = args.chatId
        loadMessageJob?.cancel()
        loadMessageJob = firebaseFirestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("create_at", Query.Direction.DESCENDING)
            .limit(maxMessage)
            .asFlow()
            .map { querySnapshot ->
                querySnapshot.map {
                    MessageEntry.fromDocumentSnapshot(it)
                }.let {
                    listOf(ChatUtil.WELCOME_MESSAGE) + it
                }.sortedByDescending { it.createAt }
                    .take(maxMessage.toInt())
            }
            .execute {
                copy(messages = it)
            }
    }

    fun mayBeRecordSeenLastMessage(messageEntry: MessageEntry) {
        withState { state ->
            val messages = state.messages.invoke() ?: return@withState
            val lastMessage = messages.firstOrNull()
            if (lastMessage?.id == messageEntry.id) {
                viewModelScope.launch {
                    Timber.d("Record last read: ${messageEntry.text} ${messageEntry.id}")
                    chatRepository.recordLastReadMessageId(args.chatId, messageEntry.id)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val textToSend = text.trim()
        if (textToSend.isEmpty()) {
            return
        }
        suspend {
            sendMessageInternal(textToSend)
        }.execute { copy(sendMessageTask = it) }
    }

    private suspend fun sendMessageInternal(text: String) {
        val user = ensureSignInAnonymously()
        val chatId = args.chatId
        val chatDocument = firebaseFirestore.collection("chats")
            .document(chatId)
        val messageDocument = chatDocument
            .collection("messages")
            .document()

        val message = MessageEntry(
            id = messageDocument.id,
            createAt = Instant.now(),
            text = text,
            senderUid = user.uid
        )
        val docData = message.getDocumentData()
        coroutineScope {
            val task1 = async {
                chatDocument.set(docData).await()
            }
            val task2 = async {
                messageDocument.set(docData).await()
            }
            listOf(task1, task2).awaitAll()
        }
    }

    companion object : MavericksViewModelFactory<ChatHistoryViewModel, ChatHistoryState> {
        override fun create(
            viewModelContext: ViewModelContext,
            state: ChatHistoryState
        ): ChatHistoryViewModel {
            return viewModelContext.fragment<ChatHistoryFragment>()
                .run { viewModelFactory.create(state, args) }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            initState: ChatHistoryState,
            args: ChatHistoryArgs
        ): ChatHistoryViewModel
    }
}
