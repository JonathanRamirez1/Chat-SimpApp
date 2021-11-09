package com.jonathan.chatsimpapp.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider
import com.jonathan.chatsimpapp.data.model.providers.UserProvider

class MainViewModel: ViewModel() {

    //Providers
    private val authProvider = AuthProvider()
    private val userProvider = UserProvider()

    //LiveData
    private val _isLoginView = MutableLiveData<Boolean>()
    val isLoginView: LiveData<Boolean> = _isLoginView

    fun setDestinyView() {
        val id: String = authProvider.getUid()
        if (authProvider.isEmailVerified()) {
            userProvider.getUser(id).addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val cover: String = documentSnapshot.getString("cover").toString()
                    val phone: String = documentSnapshot.getString("phone").toString()
                    val photo: String = documentSnapshot.getString("photo").toString()
                    val username: String = documentSnapshot.getString("username").toString()
                    _isLoginView.value = !(cover.isNotEmpty() && phone.isNotEmpty() && photo.isNotEmpty() && username.isNotEmpty())
                }
            }
        } else {
            _isLoginView.value = true
        }
    }
}