package com.jonathan.loginfuturo.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.Event

class RegisterViewModel(application: Application): AndroidViewModel(application) {

  /*  private val _isRegister = MutableLiveData<Boolean>()
    val isRegister: MutableLiveData<Boolean>
        get() = _isRegister

     private val _launchFragment = MutableLiveData<Event<String>>()
    val launchFragment : LiveData<Event<String>>
        get() = _launchFragment

    private val _statusMessageRegister = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = _statusMessageRegister

    private val _email = MutableLiveData<String>()
    val email: LiveData<String>
    get() = _email

    private val _password = MutableLiveData<String>()
    val password: LiveData<String>
        get() = _password

    private val firebaseAuth : FirebaseAuth = FirebaseAuth.getInstance()


    /** Inicio de sesion usando email y password **/

    fun signUpByEmail(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser!!.sendEmailVerification().addOnCompleteListener {
                        userClicksOnButton("An email has been sent to you. Confirm before logging in.")
                        _isRegister.postValue(true)
                        _email.value = email
                        _password.value = password
                    }
                } else {
                    userClicksOnButton("Unexpected error occurred. Please try again.")
                    _isRegister.postValue(false)
                }
            }
    }

    private fun userClicksOnButton(itemId: String) {
        _launchFragment.value = Event(itemId)
    }*/
}


