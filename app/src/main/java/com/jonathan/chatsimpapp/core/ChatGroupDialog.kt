package com.jonathan.chatsimpapp.core

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Application
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.core.objects.Constants
import com.jonathan.chatsimpapp.databinding.DialogGroupChatBinding
import com.jonathan.chatsimpapp.ui.viewmodels.ChatGroupViewModel
import com.jonathan.chatsimpapp.ui.viewmodels.CompleteInfoViewModel
import java.io.File
import java.util.*

class ChatGroupDialog: DialogFragment() {

    private lateinit var binding: DialogGroupChatBinding
    private lateinit var alertDialogBuilder: AlertDialog.Builder
    private lateinit var options: Array<CharSequence>

    private val chatGroupViewModel by viewModels<ChatGroupViewModel> {
        ChatGroupViewModel(requireActivity().application).createFactory()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity
        binding = DialogGroupChatBinding.inflate(layoutInflater, null, false)

        setUpAlertDialogBuilder()
        binding.circleImageViewChatGroup.setOnClickListener {
            selectOptionsImage()
        }

        return AlertDialog.Builder(activity)
            .setTitle("Datos del Grupo")
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("Crear Grupo") { _, _ ->
                val name = binding.editTextName.text.toString()
                val description = binding.editTextDescription.text.toString()
                val imageGroup = binding.circleImageViewChatGroup
                if (name.isNotEmpty() && description.isNotEmpty()) {
                    if (imageGroup != null) {

                    }

                } else {

                }
            }
            .setNegativeButton("Cancelar") { _, _ ->

            }
            .create()
    }



    private fun setUpAlertDialogBuilder() {
        alertDialogBuilder = AlertDialog.Builder(context)
        alertDialogBuilder.setTitle(getString(R.string.choose))
        options = arrayOf(getString(R.string.gallery_image), getString(R.string.camera_photo))
    }

    private fun selectOptionsImage() {
        alertDialogBuilder.setItems(options) { _, i ->
            if (i == 0) {
                openGallery()
            } else if (i == 1) {
                openCamera()
            }
        }
        alertDialogBuilder.show()
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_GET_CONTENT)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, Constants.REQUEST_CODE_GALLERY_GROUP)
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (activity?.packageManager?.let { intent.resolveActivity(it) } != null) {
            var cameraFile: File? = null
            try {
                cameraFile = createGroupFile()
            } catch (e: Exception) {
                Toast.makeText(context, "Hubo un error en el archivo" + e.message, Toast.LENGTH_LONG).show()
            }
            if (cameraFile != null) {
                val photoUri = context?.let { FileProvider.getUriForFile(it,"com.jonathan.chatsimpapp", cameraFile) }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, Constants.REQUEST_CODE_CAMERA_GROUP)
            }
        }
    }

    /**Crea el archivo desde la camara**/
    private fun createGroupFile(): File {
        val storageDir: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val cameraFile = File.createTempFile(
            Date().toString() + "_photo",
            ".jpg",
            storageDir
        )
        return chatGroupViewModel.groupFile(Constants.REQUEST_CODE_CAMERA_GROUP, cameraFile)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imageViewGroup = binding.circleImageViewChatGroup
        chatGroupViewModel.onActivityResult(requestCode, resultCode, data, imageViewGroup)
    }
}