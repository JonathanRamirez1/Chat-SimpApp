package com.jonathan.loginfuturo.providers

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.jonathan.loginfuturo.Utils.CompressorBitmapImage
import java.io.File
import java.util.*

class ImageProvider {

    private var storageReference: StorageReference = FirebaseStorage.getInstance().reference

    fun saveImageProfile(context: Context, file: File): UploadTask {
        val imageByte = CompressorBitmapImage.getImage(context, file.path, 500, 500)
        val storageProfile: StorageReference = FirebaseStorage.getInstance().reference.child(Date().toString() + ".jpg")
        storageReference = storageProfile
        return storageProfile.putBytes(imageByte)

    }

    fun getStorage(): StorageReference {
        return storageReference
    }
}