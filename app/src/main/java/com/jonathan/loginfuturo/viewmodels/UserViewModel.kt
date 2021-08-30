package com.jonathan.loginfuturo.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.jonathan.loginfuturo.UserInformation

class UserViewModel(application: Application): AndroidViewModel(application) {

    private val _isRegister = MutableLiveData<Boolean>()
    val isRegister: MutableLiveData<Boolean>
        get() = _isRegister

    private fun setUserInformation(userInformation: UserInformation) {

    }
}