package com.jonathan.chatsimpapp.data.model.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.*
import com.jonathan.chatsimpapp.data.model.UserModel
import kotlin.collections.HashMap

class UserProvider {

    /**Crea una coleccion en Cloud Firestore**/
    private val userCollection: CollectionReference = FirebaseFirestore.getInstance().collection("UsersRegister")

    fun getUser(id: String): Task<DocumentSnapshot> {
        return userCollection.document(id).get()
    }

    fun getUserRealTime(id: String): DocumentReference {
        return userCollection.document(id)
    }

    fun createCollection(userModel: UserModel): Task<Void> {
       return userCollection.document(userModel.getId()).set(userModel)
    }

    fun completeUserInfo(userModel: UserModel): Task<Void> {
        val complete: MutableMap<String, Any> = HashMap()
        complete["cover"] = userModel.getCover()
        complete["photo"] = userModel.getPhoto()
        complete["username"] = userModel.getUsername()
        complete["phone"] = userModel.getPhone()
        return userCollection.document(userModel.getId()).update(complete)
    }

    fun getAll(): Query {
        return userCollection.orderBy("email", Query.Direction.DESCENDING)
    }

    fun getUserByEmail(email: String) : Query {
        return userCollection.orderBy("email").startAt(email).endAt(email+'\uf8ff')
    }
}