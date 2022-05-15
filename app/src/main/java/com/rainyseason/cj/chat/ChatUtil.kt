package com.rainyseason.cj.chat

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.rainyseason.cj.BuildConfig
import com.rainyseason.cj.chat.history.MessageEntry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.threeten.bp.Instant

object ChatUtil {
    val ADMIN_UID = if (BuildConfig.FLAVOR == "prod") {
        "uJIQWCKAsLMf3AlY1bTjVhmJlVM2" // prod
    } else {
        "uaNW8OUnA7MWGIaYj0OsOXGp3F42" // dev
    }

    val WELCOME_MESSAGE = MessageEntry(
        id = "welcome",
        createAt = Instant.MIN,
        senderUid = ADMIN_UID,
        text = "Hi! We are the team behind Bitcoin Widget Pro. If you got any feature requests " +
            "or anything about the app feel free to tell us!"
    )

    fun getChatId(user1: String, user2: String): String {
        return arrayOf(user1, user2).sortedArray().joinToString(separator = "_")
    }

    fun isAdmin(uid: String): Boolean {
        return uid == ADMIN_UID
    }

    fun getAnonId(chatId: String): String {
        return chatId.replace(ADMIN_UID, "")
            .replace("_", "")
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun Query.asFlow(): Flow<QuerySnapshot> {
    val query = this
    return callbackFlow {
        val listener = EventListener<QuerySnapshot> { value, error ->
            if (error != null) {
                cancel(error.message ?: "Unknown", error)
            } else if (value != null) {
                trySend(value)
            }
        }
        val registration = query.addSnapshotListener(listener)
        awaitClose {
            registration.remove()
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
fun DocumentReference.asFlow(): Flow<DocumentSnapshot> {
    val ref = this
    return callbackFlow {
        val listener = EventListener<DocumentSnapshot> { value, error ->
            if (error != null) {
                cancel(error.message ?: "Unknown", error)
            } else if (value != null) {
                trySend(value)
            }
        }
        val registration = ref.addSnapshotListener(listener)
        awaitClose {
            registration.remove()
        }
    }
}
