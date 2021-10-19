package com.jonathan.loginfuturo.data.model.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.data.model.MessageModel


class MessageProvider {

     private val messageDataBaseReference: CollectionReference = FirebaseFirestore.getInstance().collection("Messages")

    fun create(messageModel: MessageModel): Task<Void> {
        val document : DocumentReference = messageDataBaseReference.document()
        messageModel.setId(document.id)
        return document.set(messageModel)
    }

    fun getMessageByChat(idChat: String): Query {
        return messageDataBaseReference.whereEqualTo("idChat", idChat).orderBy("timeStamp", Query.Direction.ASCENDING)
    }

    fun getMessageByChatEmisor(idChat: String, idEmisor: String): Query {
        return messageDataBaseReference.whereEqualTo("idChat", idChat).whereEqualTo("idEmisor", idEmisor).whereEqualTo("viewed", false)
    }

     fun countMessage(idEmisor: String): Query {
        return messageDataBaseReference.whereEqualTo("idEmisor", idEmisor)
    }

    fun getLastThreeMessageByChatEmisor(idChat: String, idEmisor: String): Query {
        return messageDataBaseReference.whereEqualTo("idChat", idChat)
            .whereEqualTo("idEmisor", idEmisor)
            .whereEqualTo("viewed", false)
            .orderBy("timeStamp", Query.Direction.DESCENDING)
            .limit(3)
    }

    fun getLastMessage(idChat: String): Query {
        return messageDataBaseReference.whereEqualTo("idChat", idChat).orderBy("timeStamp", Query.Direction.DESCENDING).limit(1)
    }

    fun getLastMessageEmisor(idChat: String, idEmisor: String): Query {
        return messageDataBaseReference.whereEqualTo("idChat", idChat)
            .whereEqualTo("idEmisor", idEmisor)
            .orderBy("timeStamp", Query.Direction.DESCENDING).limit(1)
    }

    fun updateViewed(idDocument: String, state: Boolean): Task<Void> {
        val viewed: MutableMap<String, Any> = HashMap()
        viewed["viewed"] = state
        return  messageDataBaseReference.document(idDocument).update(viewed)
    }

    fun getChat(id: String): DocumentReference {
        return messageDataBaseReference.document(id)
    }
}
