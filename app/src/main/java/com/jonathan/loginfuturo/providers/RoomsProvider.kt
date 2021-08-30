package com.jonathan.loginfuturo.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.model.Rooms

class RoomsProvider {

  //  private val roomsCollectionReference by lazy {  FirebaseFirestore.getInstance().collection("Chats2021") }
    private val roomsCollectionReference : CollectionReference = FirebaseFirestore.getInstance().collection("Chats")

   /* fun RoomsProvider() {
        FirebaseFirestore.getInstance().collection("Chats2021")
    }*/

    fun createCollectionUsers(rooms: Rooms) {
        roomsCollectionReference.document(rooms.getIdEmisor()).collection("Users2021").document(rooms.getIdReceptor()).set(rooms)
        roomsCollectionReference.document(rooms.getIdReceptor()).collection("Users2021").document(rooms.getIdEmisor()).set(rooms)

      //  roomsCollectionReference = FirebaseFirestore.getInstance().collection("Rooms")
      //  roomsCollectionReferencelet { roomsCollectionReference.document(rooms.getIdEmisor() + rooms.getIdReceptor()).set(rooms) }
    }

    fun getAll(idUser: String): Query {
        return roomsCollectionReference.let { roomsCollectionReference.whereArrayContains("ids", idUser) }
    }

    fun getOneToOneChat(emisor: String, receptor: String): Query {
        val ids: ArrayList<String> = ArrayList()
        ids.add(emisor + receptor)
        ids.add(receptor + emisor)
        return roomsCollectionReference.let { roomsCollectionReference.whereIn("id", ids) }
    }

    //TODO SOLO ES PRUEBA DE LA CARPETA 6 DEL VIDEO 7: MIN 18:12
    fun getComment(idUser: String): Query {
        return roomsCollectionReference.whereEqualTo("idUser", idUser)
    }
}
