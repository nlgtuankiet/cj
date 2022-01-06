package com.rainyseason.cj.chat

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MavericksState
import com.airbnb.mvrx.MavericksViewModel
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.Uninitialized
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rainyseason.cj.common.isUserLoginFlow
import com.rainyseason.cj.data.database.kv.KeyValueStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class ChatState(
    val isUserLoginAsync: Async<Boolean> = Uninitialized,
    val lastReadMessageIdAsync: Async<String?> = Uninitialized,
    val lastMessageIdAsync: Async<String> = Uninitialized,
) : MavericksState {
    val showChatBadge: Boolean
        get() {
            return if (isUserLoginAsync is Success) {
                val userLoggedIn = isUserLoginAsync.invoke()
                if (userLoggedIn) {
                    if (lastMessageIdAsync is Success && lastReadMessageIdAsync is Success) {
                        val lastMessageId = lastMessageIdAsync.invoke()
                        val lastReadMessageId = lastReadMessageIdAsync.invoke()
                        lastMessageId != lastReadMessageId
                    } else {
                        false
                    }
                } else {
                    // show chat badge when user not log in
                    true
                }
            } else {
                false
            }
        }
}

@Singleton
class ChatRepository @Inject constructor(
    private val keyValueStore: KeyValueStore,
    private val firebaseAuth: FirebaseAuth,
    private val firebaseFirestore: FirebaseFirestore,
) : MavericksViewModel<ChatState>(ChatState()) {

    init {
        load()
        onAsync(ChatState::isUserLoginAsync) { isUserLogin ->
            if (isUserLogin) {
                refreshLastMessage()
            }
        }
    }

    private var lastMessageJob: Job? = null
    private var lastReadMessageJob: Job? = null
    private fun refreshLastMessage() {
        val currentUserId = firebaseAuth.currentUser?.uid ?: return
        val chatId = ChatUtil.getChatId(ChatUtil.ADMIN_UID, currentUserId)
        lastMessageJob?.cancel()
        lastMessageJob = firebaseFirestore
            .collection("chats")
            .document(chatId)
            .asFlow()
            .map { it.getString("message_id") ?: ChatUtil.WELCOME_MESSAGE.id }
            .distinctUntilChanged()
            .execute { copy(lastMessageIdAsync = it) }

        lastReadMessageJob?.cancel()
        lastReadMessageJob = lastReadMessageIdFlow(chatId)
            .execute { copy(lastReadMessageIdAsync = it) }
    }

    private fun load() {
        firebaseAuth.isUserLoginFlow()
            .distinctUntilChanged()
            .execute { copy(isUserLoginAsync = it) }
    }

    fun lastReadMessageIdFlow(chatId: String): Flow<String?> {
        return keyValueStore.getStringFlow(lastReadMessageKey(chatId)).distinctUntilChanged()
    }

    suspend fun recordLastReadMessageId(chatId: String, messageId: String) {
        keyValueStore.setString(lastReadMessageKey(chatId), messageId)
    }

    fun showChatBadgeFlow(): Flow<Boolean> {
        return stateFlow.map { it.showChatBadge }.distinctUntilChanged()
    }

    private fun lastReadMessageKey(chatId: String): String {
        return "chat_last_read_$chatId"
    }
}
