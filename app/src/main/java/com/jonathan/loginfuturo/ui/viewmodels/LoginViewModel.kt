package com.jonathan.loginfuturo.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.core.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val _isViewLoading:SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isViewLoading:LiveData<Boolean> = _isViewLoading

    private val _isAuthenticated:SingleLiveEvent<String> = SingleLiveEvent()
    val isAuthenticated:LiveData<String> = _isAuthenticated

    private val _onError:SingleLiveEvent<String> = SingleLiveEvent()
    val onError:LiveData<String> = _onError

    //TODO move to repository

    private suspend fun loginWithEmailAndPassword(
        email: String,
        password: String
    ): Pair<AuthResult?, Exception?> {
        return try {
            val result =
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            Pair(result, null)
        } catch (exception: Exception) {
            Pair(null, exception)
        }
    }

    //TODO refactor
    fun logIn(email: String, password: String) {
        //TODO add validation

        _isViewLoading.value = true
        viewModelScope.launch {
           val result = loginWithEmailAndPassword(email,password)
           if(result.first!= null) {
               _isViewLoading.value = false
               result?.first?.user?.let {
                   _isAuthenticated.value = it.email
               }
           }else {
               _isViewLoading.value = false
               _onError.value = result?.second?.message?:"Ocurrió un error"
           }
        }
    }

}

//TODO create viewModelFactory

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

