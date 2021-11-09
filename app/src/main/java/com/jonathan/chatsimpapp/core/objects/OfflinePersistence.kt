package com.jonathan.chatsimpapp.core.objects

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*
import kotlin.collections.HashMap

object OfflinePersistence {

    //Crea un nodo en RealTime Database
    private val dbReference: DatabaseReference = FirebaseDatabase.getInstance().reference.child("UsersRegister")

    fun updateUserStatus(id: String, online: String, offline: String): Task<Void> {

        val currentStateMap: MutableMap<String, Any> = HashMap()
        currentStateMap["online"] = online
        currentStateMap["offline"] = offline
        currentStateMap["lastConnect"] = Date().time

       return dbReference.child(id).child("userState")
            .updateChildren(currentStateMap)
    }

    fun getUserRealTimeDatabase(id: String): DatabaseReference {
        return dbReference.child(id)
    }
}