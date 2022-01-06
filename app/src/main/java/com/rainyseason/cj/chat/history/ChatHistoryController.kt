package com.rainyseason.cj.chat.history

import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.epoxy.VisibilityState
import com.airbnb.mvrx.withState
import com.google.firebase.auth.FirebaseAuth
import com.rainyseason.cj.chat.history.view.MessageViewViewModel_
import com.rainyseason.cj.chat.history.view.messageMeEntryView
import com.rainyseason.cj.chat.history.view.messageThemEntryView
import com.rainyseason.cj.common.view.emptyView
import com.rainyseason.cj.common.view.verticalSpacerView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class ChatHistoryController @AssistedInject constructor(
    @Assisted private val viewModel: ChatHistoryViewModel,
    @Assisted private val args: ChatHistoryArgs,
    private val firebaseAuth: FirebaseAuth,
) : AsyncEpoxyController() {
    private var skipBuildSpace = true
    override fun buildModels() {
        val state = withState(viewModel) { it }
        skipBuildSpace = true
        emptyView {
            id("empty")
        }
        val currentUserId = firebaseAuth.currentUser?.uid ?: ""
        state.messages.invoke().orEmpty().forEach { messageEntry ->
            if (messageEntry.senderUid == currentUserId) {
                buildMeTextMessage(state, messageEntry)
            } else {
                buildThemTextMessage(state, messageEntry)
            }
        }
        maybeBuildSpace("end", height = 12)
    }

    private fun maybeBuildSpace(id: String, height: Int = 6) {
        if (skipBuildSpace) {
            skipBuildSpace = false
            return
        }
        verticalSpacerView {
            id("space_$id")
            height(height)
        }
    }

    private fun MessageViewViewModel_.buildTextMessage(
        state: ChatHistoryState,
        messageEntry: MessageEntry
    ) {
        id(messageEntry.id)
        text(messageEntry.text)
        messageEntry(messageEntry)
    }

    private fun buildThemTextMessage(state: ChatHistoryState, messageEntry: MessageEntry) {
        maybeBuildSpace(messageEntry.id)
        messageThemEntryView {
            buildTextMessage(state, messageEntry)
            onVisibilityStateChanged { model, _, visibilityState ->
                if (visibilityState == VisibilityState.FULL_IMPRESSION_VISIBLE) {
                    viewModel.mayBeRecordSeenLastMessage(model.messageEntry())
                }
            }
        }
    }

    private fun buildMeTextMessage(state: ChatHistoryState, messageEntry: MessageEntry) {
        maybeBuildSpace(messageEntry.id)
        messageMeEntryView {
            buildTextMessage(state, messageEntry)
            onVisibilityStateChanged { model, _, visibilityState ->
                if (visibilityState == VisibilityState.FULL_IMPRESSION_VISIBLE) {
                    viewModel.mayBeRecordSeenLastMessage(model.messageEntry())
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(
            viewModel: ChatHistoryViewModel,
            args: ChatHistoryArgs,
        ): ChatHistoryController
    }
}
