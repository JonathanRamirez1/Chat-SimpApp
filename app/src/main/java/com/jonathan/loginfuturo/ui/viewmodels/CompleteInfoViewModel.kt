package com.jonathan.loginfuturo.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.jonathan.loginfuturo.core.*
import com.jonathan.loginfuturo.core.objects.Constants
import com.jonathan.loginfuturo.core.objects.ConvertUriToFile
import com.jonathan.loginfuturo.data.model.UserModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.ImageProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import com.squareup.picasso.Picasso
import java.io.File

class CompleteInfoViewModel(application: Application): AndroidViewModel(application) {

    //Providers
    private val imageProvider = ImageProvider()
    private val userProvider = UserProvider()
    private val authProvider = AuthProvider()

    //Models
    private val userModel = UserModel()

    private val _isLoading: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _showToast: SingleLiveEvent<String> = SingleLiveEvent()
    val showToast: LiveData<String> = _showToast

    private val _usernameValid = MutableLiveData<String?>()
    val usernameValid: LiveData<String?> = _usernameValid

    private val _phoneValid = MutableLiveData<String?>()
    val phoneValid: LiveData<String?> = _phoneValid

    private val _isComplete: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isComplete: LiveData<Boolean> = _isComplete

    private var absoluteProfilePhotoGalleryPath: String = ""
    private var profilePhotoGalleryPath: String = ""
    private var absoluteCoverPhotoCameraPath: String = ""
    private var coverPhotoCameraPath: String = ""

    private var fileProfileGallery: File? = null
    private var fileCoverGallery: File? = null
    private var fileProfileCamera: File? = null
    private var fileCoverCamera: File? = null

    fun validUsername(username: String) {
        if (isValidUsername(username)) {
            _usernameValid.value = null
        } else {
            _usernameValid.value = username
        }
    }

    fun validPhone(phone: String) {
        if (isValidUsername(phone)) {
            _phoneValid.value = null
        } else {
            _phoneValid.value = phone
        }
    }

    fun validUsernameAndPhone(username: String, phone: String) {
        if (username.isNotEmpty() && phone.isNotEmpty()) {
            if (isValidUsername(username) && isValidPhoneNumber(phone)) {
                /**SELECCIONO LAS DOS IMAGENES DE GALERIA**/
                if (fileProfileGallery != null && fileCoverGallery != null) {
                    saveUserInfo(username, phone, fileProfileGallery!!, fileCoverGallery!!)
                }
                /**TOMO LAS DOS FOTOS DE LA CAMARA**/
                else if (fileProfileCamera != null && fileCoverCamera != null) {
                    saveUserInfo(username, phone, fileProfileCamera!!, fileCoverCamera!!)
                }
                /**SELECCIONO UNA IMAGEN DE GALERIA Y LA OTRA LA TOMO DESDE LA CAMARA**/
                else if (fileProfileGallery != null && fileCoverCamera != null) {
                    saveUserInfo(username, phone, fileProfileGallery!!, fileCoverCamera!!)
                }
                /**TOMO UNA FOTO DE LA CAMARA Y LA OTRA LA SELECCIONO DESDE GALERIA**/
                else if (fileProfileCamera != null && fileCoverGallery != null) {
                    saveUserInfo(username, phone, fileProfileCamera!!, fileCoverGallery!!)
                } else {
                    _showToast.value = "You need a cover photo and a profile photo to continue"
                }
            }
        } else {
            _showToast.value = "Fill in all the fields"
        }
    }

    private fun saveUserInfo(username: String, phone: String, fileProfile: File, fileCover: File) {
        imageProvider.saveImageProfile(getApplication(), fileProfile)
            .addOnCompleteListener { taskProfile ->
                if (taskProfile.isSuccessful) {
                    imageProvider.getStorage().downloadUrl.addOnSuccessListener { uriProfile ->
                        val profile: String = uriProfile.toString()
                        imageProvider.saveImageProfile(getApplication(), fileCover).addOnCompleteListener { taskCover ->
                            if (taskCover.isSuccessful) {
                                _isLoading.value = true
                                saveImageInStorage(username, phone, profile)
                            } else {
                                _isLoading.value = false
                                _showToast.value = "Cover photo failed"
                            }
                        }
                    }
                } else {
                    _isLoading.value = false
                    _showToast.value = "There was an error"
                }
            }
    }

    private fun saveImageInStorage(username: String, phone: String, profile: String) {
        imageProvider.getStorage().downloadUrl.addOnSuccessListener { uriCover ->
            val cover: String = uriCover.toString()
            val id: String = authProvider.getUid()
            userModel.setId(id)
            userModel.setCover(cover)
            userModel.setPhoto(profile)
            userModel.setUsername(username)
            userModel.setPhone(phone)
            userProvider.completeUserInfo(userModel).addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    _isComplete.value = true
                } else {
                    _isComplete.value = false
                    _showToast.value = "Data could not be saved"
                }
            }
        }
    }

     fun photoFile(requestCode: Int, cameraFile: File): File {
        if (requestCode == Constants.REQUEST_CODE_PROFILE_CAMERA) {
            profilePhotoGalleryPath = "file:" + cameraFile.absoluteFile
            absoluteProfilePhotoGalleryPath = cameraFile.absolutePath
        } else if (requestCode == Constants.REQUEST_CODE_COVER_CAMERA) {
            coverPhotoCameraPath = "file:" + cameraFile.absoluteFile
            absoluteCoverPhotoCameraPath = cameraFile.absolutePath
        }
        return cameraFile
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, imageProfile: ImageView, imageCover: ImageView) {
        /**Selecciona imagen de la galeria**/
        if(requestCode == Constants.REQUEST_CODE_GALLERY_PROFILE_PHOTO && resultCode == AppCompatActivity.RESULT_OK) {
            try {
                fileProfileCamera = null
                fileProfileGallery = data?.data?.let { ConvertUriToFile.from(getApplication(), it) }
                imageProfile.setImageBitmap(BitmapFactory.decodeFile(fileProfileGallery?.absolutePath))

            } catch (e: Exception) {
                Log.d("ERROR", "There was an error" +e.message)
                _showToast.value = "There was an error" +e.message
            }
        }
        if(requestCode == Constants.REQUEST_CODE_GALLERY_COVER_PHOTO && resultCode == AppCompatActivity.RESULT_OK) {
            try {
                fileCoverCamera = null
                fileCoverGallery =  data?.data?.let { ConvertUriToFile.from(getApplication(), it) }
                imageCover.setImageBitmap(BitmapFactory.decodeFile(fileCoverGallery?.absolutePath))

            } catch (e: Exception) {
                Log.d("ERROR", "There was an error" +e.message)
                _showToast.value = "There was an error" +e.message
            }
        }
        /**Toma una foto de la camara**/
        if (requestCode == Constants.REQUEST_CODE_PROFILE_CAMERA && resultCode == AppCompatActivity.RESULT_OK) {
            fileProfileGallery = null
            fileProfileCamera = File(absoluteProfilePhotoGalleryPath)
            Picasso.get().load(profilePhotoGalleryPath).into(imageProfile)
        }
        if (requestCode == Constants.REQUEST_CODE_COVER_CAMERA && resultCode == AppCompatActivity.RESULT_OK) {
            fileCoverGallery = null
            fileCoverCamera = File(absoluteCoverPhotoCameraPath)
            Picasso.get().load(coverPhotoCameraPath).into(imageCover)
        }
    }
}