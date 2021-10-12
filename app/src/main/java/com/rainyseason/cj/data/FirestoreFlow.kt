package com.rainyseason.cj.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@OptIn(ExperimentalCoroutinesApi::class)
fun DocumentReference.asFlow(): Flow<DocumentSnapshot> {
    return callbackFlow {
        val listener = EventListener<DocumentSnapshot> { value, error ->
            if (value != null) {
                trySend(value)
            }
            if (error != null) {
                close(error)
            }
        }
        val registration = this@asFlow.addSnapshotListener(listener)
        this.invokeOnClose {
            registration.remove()
        }
    }
}