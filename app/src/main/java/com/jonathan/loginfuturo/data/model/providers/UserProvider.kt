package com.jonathan.loginfuturo.data.model.providers

import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.data.model.UserModel
import java.util.*
import kotlin.collections.HashMap

class UserProvider {

    //Crea una coleccion en Cloud Firestore
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

    /**SI SE QUIERE ACTUALIZAR ALGUN CAMPO AGREGAR LOS MAPS IGUAL AL DE USERNAME**/
    fun updateCollection(userModel: UserModel): Task<Void> {
        val updateUser: MutableMap<String, Any> = HashMap()
        updateUser["photo"] = userModel.getPhoto()
        return userCollection.document(userModel.getId()).update(updateUser)
    }

    fun completeUserInfo(userModel: UserModel): Task<Void> {
        val complete: MutableMap<String, Any> = HashMap()
        complete["cover"] = userModel.getCover()
        complete["photo"] = userModel.getPhoto()
        complete["username"] = userModel.getUsername()
        complete["phone"] = userModel.getPhone()
        return userCollection.document(userModel.getId()).update(complete)
    }

    fun taskUser(userModel: UserModel): Task<Void> {
        return userCollection.document(userModel.getId()).set(userModel)
    }

    fun getPhoto(photo: String): Task<DocumentSnapshot> {
        return userCollection.document(photo).get()
    }


    fun getCollection(id: String): Task<DocumentSnapshot> {
        return userCollection.document(id).get()
    }

    fun getAll(): Query {
        return userCollection.orderBy("email", Query.Direction.DESCENDING)
    }

    fun getUserByEmail(email: String) : Query {
        return userCollection.orderBy("email").startAt(email).endAt(email+'\uf8ff')
    }

    //TODO USAR PARA OBTENER DOS RESULTADOS
    fun getTwo(hola:String): Query {
        return  userCollection.whereEqualTo("photo", hola).orderBy("email", Query.Direction.DESCENDING)
    }
}