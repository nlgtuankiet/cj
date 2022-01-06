package com.rainyseason.cj.chat.history

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.rainyseason.cj.common.notNull
import org.threeten.bp.Instant

data class MessageEntry(
    val id: String,
    val createAt: Instant,
    val text: String,
    val senderUid: String,
) {

    fun getDocumentData(): Map<String, Any> {
        return mapOf(
            "message_id" to id,
            "sender_uid" to senderUid,
            "text" to text,
            "create_at" to FieldValue.serverTimestamp(),
        )
    }

    companion object {
        fun fromDocumentSnapshot(documentSnapshot: DocumentSnapshot): MessageEntry {
            return MessageEntry(
                id = run {
                    if (documentSnapshot.contains("message_id")) {
                        documentSnapshot.getString("message_id").notNull()
                    } else {
                        documentSnapshot.id
                    }
                },
                createAt = run {
                    val serverTime = documentSnapshot.getTimestamp("create_at")
                    if (serverTime != null) {
                        Instant.ofEpochSecond(serverTime.seconds, serverTime.nanoseconds.toLong())
                    } else {
                        Instant.MAX // local message1
                    }
                },
                text = documentSnapshot.getString("text").notNull(),
                senderUid = documentSnapshot.getString("sender_uid").notNull(),
            )
        }
    }
}
