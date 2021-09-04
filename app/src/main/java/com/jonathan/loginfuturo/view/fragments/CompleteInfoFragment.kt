package com.jonathan.loginfuturo.view.fragments

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.jonathan.loginfuturo.*
import com.jonathan.loginfuturo.Utils.ConvertUriToFile
import com.jonathan.loginfuturo.databinding.FragmentCompleteInfoBinding
import com.jonathan.loginfuturo.model.UserModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.ImageProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.squareup.picasso.Picasso
import dmax.dialog.SpotsDialog
import java.io.File
import java.util.*

class CompleteInfoFragment : Fragment() {

    private lateinit var binding: FragmentCompleteInfoBinding
    private lateinit var navController: NavController
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
        userModel = UserModel()
        userProvider = UserProvider()
        authProvider = AuthProvider()
        imageProvider = ImageProvider()
        setUpAlertDialog()
        setUpAlerDialogBuilder()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_complete_info, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        validFields()
        launchFindUserFragment(view)
        changeCoverPhoto()
        changeProfilePhoto()
    }

    private fun setUpAlertDialog() {
        alertDialog = SpotsDialog.Builder()
            .setContext(context)
            .setTheme(R.style.Custom)
            .setCancelable(false)
            .build()
    }

    private fun setUpAlerDialogBuilder() {
        alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(getString(R.string.choose))
        options = arrayOf(getString(R.string.gallery_image), getString(R.string.camera_photo))
    }

    private fun validFields() {

        binding.materialEditTextCompleteUsername.validate { username ->
            if (isValidUsername(username)) {
                binding.materialEditTextCompleteUsername.error = null
            } else {
                binding.materialEditTextCompleteUsername.error = getString(R.string.username_is_no_valid)
            }
        }

        binding.materialEditTextCompletePhone.validate { phone ->
            if (isValidPhoneNumber(phone)) {
                binding.materialEditTextCompletePhone.error = null
            } else {
                binding.materialEditTextCompletePhone.error = getString(R.string.phone_number_is_no_valid)
            }
        }
    }

    private fun validateFields(view: View) {
        val username: String = binding.materialEditTextCompleteUsername.text.toString()
        val phone: String = binding.materialEditTextCompletePhone.text.toString()

        if (isValidUsername(username) && isValidPhoneNumber(phone)) {
            if (username.isNotEmpty() && phone.isNotEmpty()) {
                /**SELECCIONO LAS DOS IMAGENES DE GALERIA**/
                if (fileProfileGallery != null && fileCoverGallery != null) {
                    saveUserInfo(username, phone, fileProfileGallery!!, fileCoverGallery!!, view)
                }
                /**TOMO LAS DOS FOTOS DE LA CAMARA**/
                else if (fileProfileCamera != null && fileCoverCamera != null) {
                    saveUserInfo(username, phone, fileProfileCamera!!, fileCoverCamera!!, view)
                }
                /**SELECCIONO UNA IMAGEN DE GALERIA Y LA OTRA LA TOMO DESDE LA CAMARA**/
                else if (fileProfileGallery != null && fileCoverCamera != null) {
                    saveUserInfo(username, phone, fileProfileGallery!!, fileCoverCamera!!, view)
                }
                /**TOMO UNA FOTO DE LA CAMARA Y LA OTRA LA SELECCIONO DESDE GALERIA**/
                else if (fileProfileCamera != null && fileCoverGallery != null) {
                    saveUserInfo(username, phone, fileProfileCamera!!, fileCoverGallery!!, view)
                }
                else {
                    Toast.makeText(context, "You need a cover photo and a profile photo to continue", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Fill in all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun launchFindUserFragment(view: View) {
        val goFindUser = binding.buttonCompleteOk
        goFindUser.setOnClickListener {
            validateFields(view)
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
        alertDialogBuilder.setItems(options, DialogInterface.OnClickListener { dialogInterface, i ->
            if (i == 0) {
                if (numberImage == 1) {
                    openGallery(Constants.REQUEST_CODE_GALLERY_PROFILE_PHOTO)
                } else if(numberImage == 2) {
                    openGallery(Constants.REQUEST_CODE_GALLERY_COVER_PHOTO)
                }
            } else if(i == 1) {
                if (numberImage == 1) {
                    taskPhoto(Constants.REQUEST_CODE_PROFILE_CAMERA)
                } else if(numberImage == 2) {
                    taskPhoto(Constants.REQUEST_CODE_COVER_CAMERA)
                }
            }
        })
        alertDialogBuilder.show()
    }

    private fun taskPhoto(requestCode: Int) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (activity?.let { intent.resolveActivity(it.packageManager) } != null) {
            var cameraFile: File? = null
            try {
                cameraFile = createPhotoFile(requestCode)

            } catch (e: Exception) {
                Toast.makeText(context, "Hubo un error en el archivo" + e.message, Toast.LENGTH_LONG).show()

            }
            if (cameraFile != null) {
                val photoUri = FileProvider.getUriForFile(requireContext(),"com.jonathan.loginfuturo", cameraFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, requestCode)
            }
        }
    }

    private fun createPhotoFile(requestCode: Int): File? {
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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

    private fun saveUserInfo(username: String, phone: String, fileProfile: File, fileCover: File, view: View) {
        alertDialog.show()
        imageProvider.saveImageProfile(requireContext(), fileProfile)
            .addOnCompleteListener { taskProfile ->
                if (taskProfile.isSuccessful) {
                    imageProvider.getStorage().downloadUrl.addOnSuccessListener { uriProfile ->
                        val profile: String = uriProfile.toString()
                        imageProvider.saveImageProfile(requireContext(), fileCover)
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
                                        userProvider.CompleteUserInfo(userModel).addOnCompleteListener { task ->
                                                alertDialog.dismiss()
                                                if (task.isSuccessful) {
                                                    navController = Navigation.findNavController(view)
                                                    navController.navigate(R.id.searchBarFragment)
                                                } else {
                                                    Toast.makeText(context, "Data could not be saved", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                                } else {
                                    alertDialog.dismiss()
                                    Toast.makeText(context, "Cover photo failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                } else {
                    alertDialog.dismiss()
                    Toast.makeText(context, "There was an error", Toast.LENGTH_SHORT).show()
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
                fileProfileGallery = context?.let { data?.data?.let { it1 -> ConvertUriToFile.from(it, it1) } }
                binding.circleCompletePhoto.setImageBitmap(BitmapFactory.decodeFile(fileProfileGallery?.absolutePath))

            } catch (e: Exception) {
                Log.d("ERROR", "There was an error" +e.message)
                Toast.makeText(context, "There was an error" +e.message, Toast.LENGTH_LONG).show()
            }
        }
        if(requestCode == Constants.REQUEST_CODE_GALLERY_COVER_PHOTO && resultCode == RESULT_OK) {
            try {
                fileCoverCamera = null
                fileCoverGallery = context?.let { data?.data?.let { it1 -> ConvertUriToFile.from(it, it1) } }
                binding.imageViewCompleteCover.setImageBitmap(BitmapFactory.decodeFile(fileCoverGallery?.absolutePath))

            } catch (e: Exception) {
                Log.d("ERROR", "There was an error" +e.message)
                Toast.makeText(context, "There was an error" +e.message, Toast.LENGTH_LONG).show()
            }
        }
        if (requestCode == Constants.REQUEST_CODE_PROFILE_CAMERA && resultCode == RESULT_OK) {
            fileProfileGallery = null
            fileProfileCamera = File(absoluteProfilePhotoGalleryPath)
            Picasso.get().load(profilePhotoGalleryPath).into(binding.circleCompletePhoto)
        }
        if (requestCode == Constants.REQUEST_CODE_COVER_CAMERA && resultCode == RESULT_OK) {
            fileCoverGallery = null
            fileCoverCamera = File(absoluteCoverPhotoCameraPath)
            Picasso.get().load(coverPhotoCameraPath).into(binding.imageViewCompleteCover)
        }
    }
}