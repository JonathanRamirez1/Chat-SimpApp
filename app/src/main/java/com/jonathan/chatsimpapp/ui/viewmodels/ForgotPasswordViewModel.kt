package com.jonathan.chatsimpapp.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.jonathan.chatsimpapp.core.SingleLiveEvent
import com.jonathan.chatsimpapp.core.isValidEmail
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider

class ForgotPasswordViewModel : ViewModel() {

    //Providers
    private val authProvider = AuthProvider()

    private val _emailValid: SingleLiveEvent<String> = SingleLiveEvent()
    val emailValid: LiveData<String> = _emailValid

    private val _isResetPassword: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isResetPassword: LiveData<Boolean> = _isResetPassword

    private val _showToast: SingleLiveEvent<String> = SingleLiveEvent()
    val showToast: LiveData<String> = _showToast

    fun validEmail(email: String) {
        if (isValidEmail(email)) {
            _emailValid.value = null
        } else {
            _emailValid.value = email
        }
    }

    fun onResetPassword(email: String) {
        if (isValidEmail(email)) {
            authProvider.resetPassword(email).addOnCompleteListener {
                _isResetPassword.value = true
                _showToast.value = "Email has been sent to reset your password"
            }
        } else {
            _isResetPassword.value = false
            _showToast.value = "Please make sure the email is correct"
        }
    }
}