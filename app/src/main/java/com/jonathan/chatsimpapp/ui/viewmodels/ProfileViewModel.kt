package com.jonathan.chatsimpapp.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.CircleTransform
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider
import com.jonathan.chatsimpapp.data.model.providers.UserProvider
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import java.text.SimpleDateFormat
import java.util.*

class ProfileViewModel : ViewModel() {

    //Providers
    private var authProvider = AuthProvider()
    private var userProvider = UserProvider()

    private var profileRegistration: ListenerRegistration? = null

    private val _emailProfile = MutableLiveData<String>()
    val emailProfile: LiveData<String> = _emailProfile

    private val _usernameProfile = MutableLiveData<String>()
    val usernameProfile: LiveData<String> = _usernameProfile

    private val _phoneProfile = MutableLiveData<String>()
    val phoneProfile: LiveData<String> = _phoneProfile

    private val _dateProfile = MutableLiveData<String>()
    val dateProfile: LiveData<String> = _dateProfile

    private val _coverProfile = MutableLiveData<RequestCreator>()
    val coverProfile: LiveData<RequestCreator> = _coverProfile

    private val _photoProfile = MutableLiveData<RequestCreator>()
    val photoProfile: LiveData<RequestCreator> = _photoProfile

    fun getUser() {
        profileRegistration = userProvider.getUserRealTime(authProvider.getUid())
            .addSnapshotListener { documentSnapshot, _ ->
                if (documentSnapshot != null) {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("email")) {
                            val email: String = documentSnapshot.getString("email").toString()
                            _emailProfile.value = email
                        }
                        if (documentSnapshot.contains("username")) {
                            val username: String = documentSnapshot.getString("username").toString()
                            _usernameProfile.value = username
                        }
                        if (documentSnapshot.contains("phone")) {
                            val phone: String = documentSnapshot.getString("phone").toString()
                            _phoneProfile.value = phone
                        }
                        if (documentSnapshot.contains("timeStamp")) {
                            val timeStamp: Date? = documentSnapshot.getDate("timeStamp")
                            _dateProfile.value = SimpleDateFormat("dd MMMM, yyyy").format(timeStamp)
                        }
                        if (documentSnapshot.contains("cover")) {
                            val cover: String = documentSnapshot.getString("cover").toString()
                            if (cover.isEmpty()) {
                                _coverProfile.value = Picasso.get().load(R.drawable.person)
                            } else {
                                _coverProfile.value = Picasso.get().load(cover)
                            }
                        }
                        if (documentSnapshot.contains("photo")) {
                            val photo: String = documentSnapshot.getString("photo").toString()
                            if (photo.isEmpty()) {
                                _photoProfile.value = Picasso.get().load(R.drawable.person)
                            } else {
                                _photoProfile.value = Picasso.get().load(photo).resize(100, 100)
                                    .centerCrop().transform(CircleTransform())
                            }
                        }
                    }
                }
            }
    }

    fun getProfileListener(): ListenerRegistration? {
        return profileRegistration
    }
}