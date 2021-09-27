package com.jonathan.loginfuturo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class LoginViewModel(application: Application): AndroidViewModel(application) {


}

/*   private val statusMessage: MutableLiveData<Event<String>> = MutableLiveData()
    val message: LiveData<Event<String>>
        get() = statusMessage

    private val _isLogin = MutableLiveData<Boolean>()
    val isLogin: MutableLiveData<Boolean>
        get() = _isLogin

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var firebaseUser: FirebaseUser

    fun logInByEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.isSuccessful) {
                        if (firebaseAuth.currentUser!!.isEmailVerified) {
                            _isLogin.postValue(true)

                        } else {
                            Event("User must confirm email first")
                            _isLogin.postValue(false)
                        }
                    } else {
                        Event("Sign In Failure. Authentication failed")
                        _isLogin.postValue(false)
                    }
                }
            }
    }*/

