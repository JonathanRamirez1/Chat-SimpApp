package com.jonathan.loginfuturo.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.model.Message


class MessageProvider {

     private lateinit var messageDataBaseReference: CollectionReference

    fun messageProvider() {
        messageDataBaseReference = FirebaseFirestore.getInstance().collection("Messages")
    }

    fun create(message: Message): Task<Void> {
        messageDataBaseReference = FirebaseFirestore.getInstance().collection("Messages")
        val document : DocumentReference = messageDataBaseReference.document()
        message.setId(document.id)
        return document.set(message)
    }

    fun getMessageByChat(idChat: String): Query {
        return messageDataBaseReference.whereEqualTo("idChat", idChat).orderBy("timeStamp", Query.Direction.ASCENDING)
    }
}
