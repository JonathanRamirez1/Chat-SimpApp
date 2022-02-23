package com.jonathan.chatsimpapp.data.model.providers

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.chatsimpapp.data.model.ChatGroupModel

class ChatGroupProvider {

    //Crea una colección en Firestore Database
    private val chatGroupCollectionReference : CollectionReference = FirebaseFirestore.getInstance().collection("ChatGroup")

    fun createChatGroup(chatGroupModel: ChatGroupModel) {
        chatGroupCollectionReference.document(chatGroupModel.getIdGroup()).set(chatGroupModel)
    }

    fun getChatGroup(idUsers: ArrayList<String>): Query {
        return chatGroupCollectionReference.whereIn("idUsers", idUsers)
    }
}