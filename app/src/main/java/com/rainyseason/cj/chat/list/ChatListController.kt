package com.rainyseason.cj.chat.list

import androidx.navigation.findNavController
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.withState
import com.rainyseason.cj.R
import com.rainyseason.cj.chat.ChatUtil
import com.rainyseason.cj.chat.history.ChatHistoryArgs
import com.rainyseason.cj.chat.list.view.chatEntryView
import com.rainyseason.cj.common.asArgs
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ChatListController @AssistedInject constructor(
    @Assisted private val viewModel: ChatListViewModel,
) : AsyncEpoxyController() {
    override fun buildModels() {
        val state = withState(viewModel) { it }
        state.chatList.invoke().orEmpty()
            .sortedByDescending { it.lastMessage.createAt }
            .forEach { chatEntry ->
                val lastMessageId = state.lastReadMessageId[chatEntry.id]?.invoke()
                chatEntryView {
                    id(chatEntry.id)
                    title(getTitle(chatEntry.id))
                    content(chatEntry.lastMessage.text)
                    isSeen(lastMessageId == chatEntry.lastMessage.id)
                    onClickListener { view ->
                        val args = ChatHistoryArgs(chatEntry.id)
                        view.findNavController().navigate(R.id.chat_history_screen, args.asArgs())
                    }
                }
            }
    }

    private fun getTitle(chatId: String): String {
        return ChatUtil.getAnonId(chatId).takeLast(4)
    }

    @AssistedFactory
    interface Factory {
        fun create(viewModel: ChatListViewModel): ChatListController
    }
}
