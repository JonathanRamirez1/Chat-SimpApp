package com.jonathan.loginfuturo.data.model.providers

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.data.model.ChatModel

class ChatProvider {

    private val chatCollectionReference : CollectionReference = FirebaseFirestore.getInstance().collection("Chats")

    fun createCollectionUsers(chatModel: ChatModel) {
        chatCollectionReference.document(chatModel.getIdEmisor() + chatModel.getIdReceptor()).set(chatModel)
    }

    fun getAll(idUser: String): Query {
        return chatCollectionReference.whereArrayContains("ids", idUser)
    }

    fun getOneToOneChat(idEmisor: String, idReceptor: String): Query {
        val ids: ArrayList<String> = ArrayList()
        ids.add(idEmisor + idReceptor)
        ids.add(idReceptor + idEmisor)
        return chatCollectionReference.whereIn("id", ids)
    }
}
