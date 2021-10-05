package com.jonathan.loginfuturo.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.jonathan.loginfuturo.core.*
import  com.jonathan.loginfuturo.data.model.providers.AuthProvider

class RegisterViewModel: ViewModel() {

    //Providers
    private val authProvider = AuthProvider()

    private val _isRegister: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isRegister: LiveData<Boolean> = _isRegister

    private val _showToast : SingleLiveEvent<String> = SingleLiveEvent()
    val showToast : LiveData<String> = _showToast

    private val _isEmailValid: SingleLiveEvent<String> =  SingleLiveEvent()
    val isEmailValid: LiveData<String> = _isEmailValid

    private val _isPasswordValid: SingleLiveEvent<String> =  SingleLiveEvent()
    val isPasswordValid: LiveData<String> = _isPasswordValid

    private val _isConfirmPasswordValid: SingleLiveEvent<String> =  SingleLiveEvent()
    val isConfirmPasswordValid: LiveData<String> = _isConfirmPasswordValid

    fun validEmail(email: String) {
        if (isValidEmail(email)) {
            _isEmailValid.value = null
        } else {
            _isEmailValid.value = email
        }
    }

    fun validPassword(password: String) {
        if (isValidPassword(password)) {
            _isPasswordValid.value = null
        } else {
            _isPasswordValid.value = password
        }
    }

    fun validConfirmPassword(password: String, confirmPassword: String) {
        if (isValidConfirmPassword(password, confirmPassword)) {
            _isConfirmPasswordValid.value = null
        } else {
            _isConfirmPasswordValid.value = confirmPassword
        }
    }

    fun onRegister(email: String, password: String, confirmPassword: String) {
        if (isValidEmail(email) && isValidPassword(password) && isValidConfirmPassword(password, confirmPassword)) {
            signUpByEmail(email, password)
        } else {
            _showToast.value = "Please make sure all data is correct"
        }
    }

    private fun signUpByEmail(email: String, password: String) {
        authProvider.register(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                authProvider.sendEmailVerification().addOnCompleteListener { verifyEmail ->
                    if (verifyEmail.isSuccessful) {
                        _isRegister.value = true
                        _showToast.value = "An email has been sent to you. Confirm before logging in"
                    } else {
                        _isRegister.value = false
                    }
                }
            } else {
                _isRegister.value = false
                _showToast.value = "This email is already registered"
            }
        }
    }
}


