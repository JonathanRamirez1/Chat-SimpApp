package com.jonathan.chatsimpapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Reduce y comprime una imagen elegida por el usuario antes de subirla a Storage. */
object ImageCompressor {

    suspend fun compress(context: Context, uri: Uri, maxSize: Int = 1600, quality: Int = 82): ByteArray =
        withContext(Dispatchers.IO) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val largest = max(info.size.width, info.size.height)
                if (largest > maxSize) {
                    val scale = maxSize.toFloat() / largest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bitmap.recycle()
                out.toByteArray()
            }
        }
}
