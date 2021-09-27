package com.jonathan.loginfuturo.data.model.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jonathan.loginfuturo.data.model.Token
import com.google.firebase.messaging.FirebaseMessaging

class TokenProvider {

    private val tokenCollectionReference : CollectionReference = FirebaseFirestore.getInstance().collection("Tokens")

    fun create(idUser: String?) {
        if (idUser == null) {
            return
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { instanceIdResult ->  //TODO 7 2; 4:45
            val token = Token(instanceIdResult.result)
            tokenCollectionReference.document(idUser).set(token)
        }
    }

    fun getToken(idUser: String): Task<DocumentSnapshot?> {
        return tokenCollectionReference.document(idUser).get()
    }

}