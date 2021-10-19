package com.jonathan.loginfuturo.core.objects

import android.content.Context
import android.graphics.Bitmap
import id.zelory.compressor.Compressor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

object CompressorBitmapImage {
    /*
     * Metodo que permite comprimir imagenes y transformarlas a bitmap
     */
    fun getImage(ctx: Context?, path: String, width: Int, height: Int): ByteArray {
        val fileThumbPath = File(path)
        var thumbBitmap: Bitmap? = null
        try {
            thumbBitmap = Compressor(ctx)
                .setMaxWidth(width)
                .setMaxHeight(height)
                .setQuality(75)
                .compressToBitmap(fileThumbPath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val baos = ByteArrayOutputStream()
        thumbBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        return baos.toByteArray()
    }
}