package com.jonathan.loginfuturo.ui.view.activities

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.core.ConvertUriToFile.from
import com.jonathan.loginfuturo.core.Constants
import com.jonathan.loginfuturo.core.isValidPhoneNumber
import com.jonathan.loginfuturo.core.isValidUsername
import com.jonathan.loginfuturo.core.validate
import com.jonathan.loginfuturo.data.model.UserModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.ImageProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import com.squareup.picasso.Picasso
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_complete_info.*
import java.io.File
import java.util.*

class CompleteInfoActivity : AppCompatActivity() {

    private lateinit var userModel: UserModel
    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var imageProvider: ImageProvider
    private lateinit var alertDialog: AlertDialog
    private lateinit var alertDialogBuilder: AlertDialog.Builder
    private lateinit var options: Array<CharSequence>

    private var absoluteProfilePhotoGalleryPath: String = ""
    private var profilePhotoGalleryPath: String = ""
    private var absoluteCoverPhotoCameraPath: String = ""
    private var coverPhotoCameraPath: String = ""

    private var fileProfileGallery: File? = null
    private var fileCoverGallery: File? = null
    private var fileProfileCamera: File? = null
    private var fileCoverCamera: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_info)

        /** Toolbar **/
        setSupportActionBar(findViewById(R.id.toolbarView))

        userModel = UserModel()
        userProvider = UserProvider()
        authProvider = AuthProvider()
        imageProvider = ImageProvider()

        setUpAlertDialog()
        setUpAlerDialogBuilder()
        validFields()
        launchFindUserActivity()
        changeCoverPhoto()
        changeProfilePhoto()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.general_options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuLogOut -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpAlertDialog() {
        alertDialog = SpotsDialog.Builder()
            .setContext(this)
            .setTheme(R.style.Custom)
            .setCancelable(false)
            .build()
    }

    private fun setUpAlerDialogBuilder() {
        alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.choose))
        options = arrayOf(getString(R.string.gallery_image), getString(R.string.camera_photo))
    }

    private fun validFields() {

        materialEditTextCompleteUsername.validate { username ->
            if (isValidUsername(username)) {
                materialEditTextCompleteUsername.error = null
            } else {
                materialEditTextCompleteUsername.error = getString(R.string.username_is_no_valid)
            }
        }

        materialEditTextCompletePhone.validate { phone ->
            if (isValidPhoneNumber(phone)) {
                materialEditTextCompletePhone.error = null
            } else {
                materialEditTextCompletePhone.error = getString(R.string.phone_number_is_no_valid)
            }
        }
    }

    private fun validateFields() {
        val username: String = materialEditTextCompleteUsername.text.toString()
        val phone: String = materialEditTextCompletePhone.text.toString()
        val gender: String = materialEditTextCompleteGender.text.toString()

        if (username.isNotEmpty() && phone.isNotEmpty() && gender.isNotEmpty()) {
            if (isValidUsername(username) && isValidPhoneNumber(phone)) {
                /**SELECCIONO LAS DOS IMAGENES DE GALERIA**/
                if (fileProfileGallery != null && fileCoverGallery != null) {
                    saveUserInfo(username, phone, gender, fileProfileGallery!!, fileCoverGallery!!)
                }
                /**TOMO LAS DOS FOTOS DE LA CAMARA**/
                else if (fileProfileCamera != null && fileCoverCamera != null) {
                    saveUserInfo(username, phone, gender, fileProfileCamera!!, fileCoverCamera!!)
                }
                /**SELECCIONO UNA IMAGEN DE GALERIA Y LA OTRA LA TOMO DESDE LA CAMARA**/
                else if (fileProfileGallery != null && fileCoverCamera != null) {
                    saveUserInfo(username, phone, gender, fileProfileGallery!!, fileCoverCamera!!)
                }
                /**TOMO UNA FOTO DE LA CAMARA Y LA OTRA LA SELECCIONO DESDE GALERIA**/
                else if (fileProfileCamera != null && fileCoverGallery != null) {
                    saveUserInfo(username, phone, gender, fileProfileCamera!!, fileCoverGallery!!)
                }
                else {
                    Toast.makeText(this, "You need a cover photo and a profile photo to continue", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "Fill in all the fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchFindUserActivity() {
        val goFindUser = buttonCompleteOk
        goFindUser.setOnClickListener {
            validateFields()
        }
    }

    private fun changeProfilePhoto() {
        val gallery = buttonCompletePhoto

        gallery.setOnClickListener {
            selectOptionsImage(1)
        }
    }

    private fun changeCoverPhoto() {
        val gallery = buttonCompleteCover

        gallery.setOnClickListener {
            selectOptionsImage(2)
        }
    }

    private fun selectOptionsImage(numberImage: Int) {
        alertDialogBuilder.setItems(options) { _, i ->
            if (i == 0) {
                if (numberImage == 1) {
                    openGallery(Constants.REQUEST_CODE_GALLERY_PROFILE_PHOTO)
                } else if (numberImage == 2) {
                    openGallery(Constants.REQUEST_CODE_GALLERY_COVER_PHOTO)
                }
            } else if (i == 1) {
                if (numberImage == 1) {
                    openCamera(Constants.REQUEST_CODE_PROFILE_CAMERA)
                } else if (numberImage == 2) {
                    openCamera(Constants.REQUEST_CODE_COVER_CAMERA)
                }
            }
        }
        alertDialogBuilder.show()
    }

    private fun openCamera(requestCode: Int) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            var cameraFile: File? = null
            try {
                cameraFile = createPhotoFile(requestCode)

            } catch (e: Exception) {
                Toast.makeText(this, "Hubo un error en el archivo" + e.message, Toast.LENGTH_LONG).show()

            }
            if (cameraFile != null) {
                val photoUri = FileProvider.getUriForFile(this,"com.jonathan.loginfuturo", cameraFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, requestCode)
            }
        }
    }

    private fun createPhotoFile(requestCode: Int): File? {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val cameraFile = File.createTempFile(
            Date().toString() + "_photo",
            ".jpg",
            storageDir
        )
        if (requestCode == Constants.REQUEST_CODE_PROFILE_CAMERA) {
            profilePhotoGalleryPath = "file:" + cameraFile.absoluteFile
            absoluteProfilePhotoGalleryPath = cameraFile.absolutePath
        } else if (requestCode == Constants.REQUEST_CODE_COVER_CAMERA) {
            coverPhotoCameraPath = "file:" + cameraFile.absoluteFile
            absoluteCoverPhotoCameraPath = cameraFile.absolutePath
        }
        return cameraFile
    }

    private fun saveUserInfo(username: String, phone: String, gender: String, fileProfile: File, fileCover: File) {
        alertDialog.show()
        imageProvider.saveImageProfile(this, fileProfile)
            .addOnCompleteListener { taskProfile ->
                if (taskProfile.isSuccessful) {
                    imageProvider.getStorage().downloadUrl.addOnSuccessListener { uriProfile ->
                        val profile: String = uriProfile.toString()
                        imageProvider.saveImageProfile(this, fileCover)
                            .addOnCompleteListener { taskCover ->
                                if (taskCover.isSuccessful) {
                                    imageProvider.getStorage().downloadUrl.addOnSuccessListener { uriCover ->
                                        val cover: String = uriCover.toString()
                                        val id: String = authProvider.getUid()
                                        userModel.setId(id)
                                        userModel.setCover(cover)
                                        userModel.setPhoto(profile)
                                        userModel.setUsername(username)
                                        userModel.setPhone(phone)
                                        userModel.setGender(gender)
                                        userProvider.completeUserInfo(userModel).addOnCompleteListener { task ->
                                            alertDialog.dismiss()
                                            if (task.isSuccessful) {
                                                val intent = Intent(this, FindUserActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                            } else {
                                                Toast.makeText(this, "Data could not be saved", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    alertDialog.dismiss()
                                    Toast.makeText(this, "Cover photo failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    alertDialog.dismiss()
                    Toast.makeText(this, "There was an error", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun openGallery(requestCode: Int) {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        /**Selecciona imagen de la galeria**/
        if(requestCode == Constants.REQUEST_CODE_GALLERY_PROFILE_PHOTO && resultCode == RESULT_OK) {
            try {
                fileProfileCamera = null
                fileProfileGallery = data?.data?.let { from(this, it) }
                circleCompletePhoto.setImageBitmap(BitmapFactory.decodeFile(fileProfileGallery?.absolutePath))

            } catch (e: Exception) {
                Log.d("ERROR", "There was an error" +e.message)
                Toast.makeText(this, "There was an error" +e.message, Toast.LENGTH_LONG).show()
            }
        }
        if(requestCode == Constants.REQUEST_CODE_GALLERY_COVER_PHOTO && resultCode == RESULT_OK) {
            try {
                fileCoverCamera = null
                fileCoverGallery =  data?.data?.let { from(this, it) }
                imageViewCompleteCover.setImageBitmap(BitmapFactory.decodeFile(fileCoverGallery?.absolutePath))

            } catch (e: Exception) {
                Log.d("ERROR", "There was an error" +e.message)
                Toast.makeText(this, "There was an error" +e.message, Toast.LENGTH_LONG).show()
            }
        }
        if (requestCode == Constants.REQUEST_CODE_PROFILE_CAMERA && resultCode == RESULT_OK) {
            fileProfileGallery = null
            fileProfileCamera = File(absoluteProfilePhotoGalleryPath)
            Picasso.get().load(profilePhotoGalleryPath).into(circleCompletePhoto)
        }
        if (requestCode == Constants.REQUEST_CODE_COVER_CAMERA && resultCode == RESULT_OK) {
            fileCoverGallery = null
            fileCoverCamera = File(absoluteCoverPhotoCameraPath)
            Picasso.get().load(coverPhotoCameraPath).into(imageViewCompleteCover)
        }
    }
}