package com.jonathan.loginfuturo.providers

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.jonathan.loginfuturo.model.UserModel

class AuthProvider {

    //TODO ALGUN ERROR MIRAR CARPETA 3 VIDEO 1
  //  private lateinit var firebaseUser: FirebaseUser
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private var firebaseUser: FirebaseUser? = null
    private val userModel = UserModel()
    private val userProvider = UserProvider()

    fun login(email: String, password: String): Task<AuthResult> {
        return firebaseAuth.signInWithEmailAndPassword(email, password)
    }

    fun register(email: String, password: String): Task<AuthResult> {
        return firebaseAuth.createUserWithEmailAndPassword(email, password)
    }

    fun googleLogin(googleSingAccount: GoogleSignInAccount): Task<AuthResult> {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(googleSingAccount.idToken, null)
        return firebaseAuth.signInWithCredential(credential)
    }

    fun getUid(): String {
        return if (firebaseAuth.currentUser != null) {
            firebaseAuth.currentUser!!.uid
        } else {
            null.toString()
        }
    }

    fun getEmail(): String {
        return if (firebaseAuth.currentUser != null) {
            firebaseAuth.currentUser!!.email.toString()
        } else {
            null.toString()
        }
    }

    fun getPhoto(): String {
        return firebaseUser?.photoUrl.toString()
    }

    fun isEmailVerified(): Boolean {
       return firebaseAuth.currentUser!!.isEmailVerified
    }

     fun sendEmailVerification(): Task<Void> {
        return firebaseAuth.currentUser!!.sendEmailVerification()
    }

    fun setUPCurrentUser() {
        firebaseUser = firebaseAuth.currentUser!!
    }

     fun updatePhoto() {
        val photo = firebaseUser?.photoUrl
        val id: String = getUid()
        userModel.setPhoto(photo.toString())
        userModel.setId(id)
        userProvider.updateCollection(userModel)
    }
}