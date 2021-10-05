package com.jonathan.loginfuturo.data.model.providers

import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.jonathan.loginfuturo.data.model.UserModel

class AuthProvider {

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

    fun facebookLogin(token: AccessToken): Task<AuthResult> {
        val credential = FacebookAuthProvider.getCredential(token.token)
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
       return firebaseAuth.currentUser?.isEmailVerified?:false
    }

     fun sendEmailVerification(): Task<Void> {
        return firebaseAuth.currentUser?.sendEmailVerification()?:throw Exception("horror!")
    }

    fun setUPCurrentUser() {
        firebaseUser = firebaseAuth.currentUser?:throw Exception("horror!")
    }

     fun updatePhoto() {
        val photo = firebaseUser?.photoUrl
        val id: String = getUid()
        userModel.setPhoto(photo.toString())
        userModel.setId(id)
        userProvider.updateCollection(userModel)
    }
}