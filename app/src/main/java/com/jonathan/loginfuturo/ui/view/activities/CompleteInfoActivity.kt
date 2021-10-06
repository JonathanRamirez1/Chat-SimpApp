package com.jonathan.loginfuturo.ui.view.activities

import android.annotation.SuppressLint
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
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.core.*
import com.jonathan.loginfuturo.core.ConvertUriToFile.from
import com.jonathan.loginfuturo.core.ext.createFactory
import com.jonathan.loginfuturo.data.model.UserModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.ImageProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import com.jonathan.loginfuturo.databinding.ActivityCompleteInfoBinding
import com.jonathan.loginfuturo.databinding.ToolbarBinding
import com.jonathan.loginfuturo.ui.viewmodels.CompleteInfoViewModel
import com.squareup.picasso.Picasso
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_complete_info.*
import java.io.File
import java.util.*

class CompleteInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompleteInfoBinding
    private lateinit var alertDialogBuilder: AlertDialog.Builder
    private lateinit var options: Array<CharSequence>

    private val completeInfoViewModel by viewModels<CompleteInfoViewModel> {
        CompleteInfoViewModel(application).createFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompleteInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setObservers()

        /** Toolbar **/
        setSupportActionBar(findViewById(R.id.toolbarView))

        launchFindUserActivity()
        setUpAlertDialogBuilder()
        validFields()
        changeCoverPhoto()
        changeProfilePhoto()
    }

    private fun setObservers() {

        completeInfoViewModel.isLoading.observe(this) { loading ->
            val visibility = if (loading == true) setUpProgress().show() else setUpProgress().dismiss()
            Log.d("CONSOLE", "isViewLoading $visibility ")
        }

        completeInfoViewModel.showToast.observe(this) {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            Log.d("CONSOLE", "onError $it ")
        }

        completeInfoViewModel.usernameValid.observe(this) { username ->
            if (username != null) {
                binding.materialEditTextCompleteUsername.error = getString(R.string.username_is_no_valid)
            }
        }

        completeInfoViewModel.phoneValid.observe(this) { phone ->
            if (phone != null) {
                binding.materialEditTextCompletePhone.error = getString(R.string.phone_number_is_no_valid)
            }
        }

        completeInfoViewModel.isComplete.observe(this) { complete ->
            if (complete == true) {
                goToHomeView()
            }
        }
    }

    private fun goToHomeView() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
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

    private fun setUpAlertDialogBuilder() {
        alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(getString(R.string.choose))
        options = arrayOf(getString(R.string.gallery_image), getString(R.string.camera_photo))
    }

    private fun validFields() {

        binding.materialEditTextCompleteUsername.validate { username ->
            completeInfoViewModel.validUsername(username)
        }

        materialEditTextCompletePhone.validate { phone ->
            completeInfoViewModel.validPhone(phone)
        }
    }

    private fun validateFields() {
        val username: String = materialEditTextCompleteUsername.text.toString()
        val phone: String = materialEditTextCompletePhone.text.toString()
        completeInfoViewModel.validUsernameAndPhone(username, phone)
    }

    private fun launchFindUserActivity() {
        val goFindUser = binding.buttonCompleteOk
        goFindUser.setOnClickListener {
            validateFields()
        }
    }

    private fun changeProfilePhoto() {
        val gallery = binding.buttonCompletePhoto

        gallery.setOnClickListener {
            selectOptionsImage(1)
        }
    }

    private fun changeCoverPhoto() {
        val gallery = binding.buttonCompleteCover

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

    @SuppressLint("QueryPermissionsNeeded")
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

    private fun createPhotoFile(requestCode: Int): File {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val cameraFile = File.createTempFile(
            Date().toString() + "_photo",
            ".jpg",
            storageDir)
        return completeInfoViewModel.photoFile(requestCode, cameraFile)
    }

    private fun openGallery(requestCode: Int) {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imageProfile = binding.circleCompletePhoto
        val imageCover = binding.imageViewCompleteCover
       completeInfoViewModel.onActivityResult(requestCode, resultCode, data, imageProfile, imageCover)
    }
}