package com.jonathan.loginfuturo.ui.viewmodels

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.GoogleApiClient
import com.jonathan.loginfuturo.core.*
import com.jonathan.loginfuturo.core.objects.Constants
import com.jonathan.loginfuturo.data.model.UserModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import java.util.*

class LoginViewModel : ViewModel() {

    //Models
    private val userModel = UserModel()

    //Providers
    private val authProvider = AuthProvider()
    private val userProvider = UserProvider()

    private val _isDestinyView: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isDestinyView: LiveData<Boolean> = _isDestinyView

    private val _isLoading: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showToast: SingleLiveEvent<String> = SingleLiveEvent()
    val showToast: LiveData<String> = _showToast

    private val _emailValid: SingleLiveEvent<String> = SingleLiveEvent()
    val emailValid: LiveData<String> = _emailValid

    private val _isPasswordValid: SingleLiveEvent<String> = SingleLiveEvent()
    val isPasswordValid: LiveData<String> = _isPasswordValid

    private val _isLoginFacebook = MutableLiveData<Boolean>()
    val isLoginFacebook: LiveData<Boolean> = _isLoginFacebook

    private val googleSignInAccount: GoogleApiClient? = null
    private val callbackManager = CallbackManager.Factory.create()

    var email: String = ""
    var password: String = ""

    fun validEmail(email: String) {
        if (isValidEmail(email)) {
            _emailValid.value = null
        } else {
            _emailValid.value = email
        }
    }

    fun validPassword(password: String) {
        if (isValidPassword(password)) {
            _isPasswordValid.value = null
        } else {
            _isPasswordValid.value = password
        }
    }

    fun onLoginEmailAndPassword(email: String, password: String) {
        if (isValidEmail(email) && isValidPassword(password)) {
            logInByEmailAndPassword(email, password)
        } else {
            _showToast.value = "Please make sure all data is correct"
        }
    }

    private fun logInByEmailAndPassword(email: String, password: String) {
        authProvider.login(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (authProvider.isEmailVerified()) {
                    _isLoading.value = true
                    checkUserExist()
                } else {
                    _isLoading.value = false
                    _showToast.value = "User must confirm email first"
                }
            } else {
                _isLoading.value = false
                _showToast.value = "Wrong email or password"
            }
        }
    }

    /** Verifica si un usuario esta registrado en Firestore, sino lo agrega a la Base de Datos **/
    private fun checkUserExist() {
        val id: String = authProvider.getUid()
        userProvider.getUser(id).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                setDestinyView()
            } else {
                saveUserInformation(id)
            }
        }
    }

    /** Inicio de sesion con google **/
    private fun onLoginGoogle(googleAccount: GoogleSignInAccount) {
        authProvider.googleLogin(googleAccount).addOnCompleteListener { task ->
            if (googleSignInAccount != null) {
                if (googleSignInAccount.isConnected) {
                    Auth.GoogleSignInApi.signOut(googleSignInAccount)
                }
            }
            if (task.isSuccessful) {
                _isLoading.value = true
                checkUserExist()
            } else {
                _isLoading.value = false
                _showToast.value = "Could not login with google"
            }
        }
    }

    fun onLoginFacebook() {
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult?) {
                    result?.let {
                        val token = it.accessToken
                        authProvider.facebookLogin(token).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                _isLoginFacebook.value = true
                                _showToast.value = "Iniciaste Sesion con Facebook"
                            }
                        }
                    }
                }

                override fun onCancel() {
                    _isLoginFacebook.value = false
                    _showToast.value = "facebook:onCancel"
                }

                override fun onError(error: FacebookException?) {
                    _isLoginFacebook.value = false
                    _showToast.value = "facebook:onError"
                }
            })
    }

     private fun setDestinyView() {
        val id: String = authProvider.getUid()
        userProvider.getUser(id).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                val cover: String = documentSnapshot.getString("cover").toString()
                val phone: String = documentSnapshot.getString("phone").toString()
                val photo: String = documentSnapshot.getString("photo").toString()
                val username: String = documentSnapshot.getString("username").toString()
                _isDestinyView.value = !(cover.isEmpty() && phone.isEmpty() && photo.isEmpty() && username.isEmpty())
            }
        }
    }

    /**Guarda los datos de usuarios registrados, en la Base de Datos de Firestore**/
    private fun saveUserInformation(id: String) {
        val username = ""
        val timeStamp = Date()
        email = authProvider.getEmail()
        userModel.setId(id)
        userModel.setEmail(email)
        userModel.setTimeStamp(timeStamp)
        userModel.setUsername(username)
        userProvider.createCollection(userModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                setDestinyView()
            } else {
                _showToast.value = "The information could not be saved"
            }
        }
    }

    fun onActivityResultForGoogle(data: Intent?, requestCode: Int) {
        if (requestCode == Constants.REQUEST_CODE_GOOGLE_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result!!.isSuccess) {
                val account = result.signInAccount
                onLoginGoogle(account!!)
            }
        }
    }

    fun onActivityResultForFacebook(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}