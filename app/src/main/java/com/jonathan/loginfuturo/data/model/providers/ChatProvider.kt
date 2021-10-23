package com.jonathan.loginfuturo.data.model.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.data.model.ChatModel

class ChatProvider {

    //Crea una colección en Firestore Database
    private val chatCollectionReference : CollectionReference = FirebaseFirestore.getInstance().collection("Chats")

    //Crea un nodo en RealTime Database
    private val dbReference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("Chats")

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

    //TODO USAR PARA ACTUALIZAR OBJETOS ANIDADOS EN FIRESTORE
    fun updateEmisor(idChat: String, idEmisor: String, idReceptor: String, writing: String): Task<Void> {
        val writingState: MutableMap<String, Any> = HashMap()
        writingState["writing"] = writing

        return chatCollectionReference.document(idChat)
            .update(
                mapOf(
                    "idEmisor.Emisor" to idEmisor,
                    "idEmisor.writing" to writing,
                    "idReceptor.Receptor" to idReceptor,
                    "idReceptor.writing" to writing
                )
            )
    }

    fun createNodeUsers(chatModel: ChatModel, id: String) {
        dbReference.child(chatModel.getIdEmisor() + chatModel.getIdReceptor()).child(id)
    }

    fun updateWriting(idChat: String, id: String, writing: String): Task<Void> {
        val writingState: MutableMap<String, Any> = HashMap()
        writingState["writing"] = writing

        return dbReference.child(idChat).child(id)
            .updateChildren(writingState)
    }

    fun getWritingRealTimeDB(idChat: String, id: String): DatabaseReference {
        return dbReference.child(idChat).child(id)
    }
}
