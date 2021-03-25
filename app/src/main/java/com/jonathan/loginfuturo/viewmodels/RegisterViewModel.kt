package com.jonathan.loginfuturo.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.BaseCommand
import com.jonathan.loginfuturo.Event
import com.jonathan.loginfuturo.SingleLiveEvent

class RegisterViewModel(application: Application): AndroidViewModel(application) {

    private val _isRegister = MutableLiveData<Boolean>()
    val isRegister: MutableLiveData<Boolean>
        get() = _isRegister

     private val _launchFragment = MutableLiveData<Event<String>>()
    val launchFragment : LiveData<Event<String>>
        get() = _launchFragment



    private val _statusMessageRegister = MutableLiveData<Event<String>>()
    val message: LiveData<Event<String>>
        get() = _statusMessageRegister

    /* private val _pruebaLaunch = MutableLiveData<Unit>()
    val pruebaLaunch : LiveData<Unit>
        get() = _pruebaLaunch*/

    val command: SingleLiveEvent<BaseCommand> = SingleLiveEvent()


    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }


    /** Inicio de sesion usando email y password **/

    fun signUpByEmail(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser!!.sendEmailVerification().addOnCompleteListener {
                        userClicksOnButton("An email has been sent to you. Confirm before logging in.")
                        _isRegister.postValue(true)
                    }
                } else {
                    userClicksOnButton("Unexpected error occurred. Please try again.")
                    _isRegister.postValue(false)
                }
            }
    }

    fun userClicksOnButton(itemId: String) {
        _launchFragment.value = Event(itemId)
    }

}


