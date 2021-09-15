package com.jonathan.loginfuturo.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jonathan.loginfuturo.model.OtherUserId

class OtherUserIdProvider {

    private val otherUserIdCollectionReference : CollectionReference = FirebaseFirestore.getInstance().collection("OtherUserId")

    fun otherId(otherUserId: OtherUserId): Task<Void> {
        return otherUserIdCollectionReference.document().set(otherUserId)
    }

    fun getOtherUserId(id: String): Task<DocumentSnapshot> {
        return otherUserIdCollectionReference.document(id).get()
    }
}