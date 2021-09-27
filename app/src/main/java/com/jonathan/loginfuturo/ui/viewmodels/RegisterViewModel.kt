package com.jonathan.loginfuturo.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.Event
import com.jonathan.loginfuturo.core.IdResourceString
import com.jonathan.loginfuturo.core.ResourceString
import com.jonathan.loginfuturo.core.SingleLiveEvent
import com.jonathan.loginfuturo.data.model.UserModel
import  com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import java.util.*

class RegisterViewModel(application: Application): AndroidViewModel(application) {

    //Providers
    private val authProvider = AuthProvider()
    private val userProvider = UserProvider()

    //Models
    private val userModel = UserModel()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean>
    get() = _isLoading

    private val _showToast = SingleLiveEvent<Event<String>>()
    val showToast : LiveData<Event<String>>
    get() = _showToast

     fun signUpByEmail(email: String, password: String) {
         _isLoading.value = true
        authProvider.register(email, password).addOnCompleteListener { task ->
            _isLoading.value = false
                if (task.isSuccessful) {
                    _isLoading.value = false
                    authProvider.sendEmailVerification().addOnCompleteListener {
                        _showToast.value = Event("An email has been sent to you. Confirm before logging in")
                    }
                } else {
                    _isLoading.value = false
                  //  _showToast.value = IdResourceString(R.string.register_error_create)
                }
            }
    }

    /**Guarda los datos de usuarios registrados, en la Base de Datos de Firestore**/
     fun saveUserInformation(email: String) {
        val id: String = authProvider.getUid()
        val timeStamp = Date()
        val username = ""
        userModel.setEmail(email)
        userModel.setId(id)
        userModel.setTimeStamp(timeStamp)
        userModel.setUsername(username)

        userProvider.createCollection(userModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
             //   _showToast.value = IdResourceString(R.string.register_user_saved)
            } else {
               // _showToast.value = IdResourceString(R.string.register_user_saved_error)
            }
        }
    }

  /*  private fun userClicksOnButton(message: String) {
        _showToast.value = SingleLiveEvent(message)
    }*/
}


