package com.jonathan.chatsimpapp.notifications

import android.graphics.Bitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.repository.AuthRepository
import com.jonathan.chatsimpapp.data.repository.UserRepository
import com.jonathan.chatsimpapp.di.ApplicationScope
import com.jonathan.chatsimpapp.util.ActiveChat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Recibe los mensajes push que envía la Cloud Function `onMessageCreated` (carpeta /functions).
 * Son mensajes "data", así que la app decide cómo mostrarlos tanto en primer como en segundo plano.
 */
@AndroidEntryPoint
class ChatMessagingService : FirebaseMessagingService() {

    @Inject lateinit var auth: AuthRepository
    @Inject lateinit var users: UserRepository
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onNewToken(token: String) {
        val uid = auth.currentUserId ?: return
        scope.launch { runCatching { users.addFcmToken(uid, token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val chatId = data["chatId"] ?: return
        if (auth.currentUserId == null || ActiveChat.chatId == chatId) return

        val senderName = data["senderName"].orEmpty()
        val body = data["body"].orEmpty().ifBlank {
            if (data["hasImage"] == "1") getString(R.string.notification_photo) else ""
        }
        // onMessageReceived corre en un hilo de fondo, se puede esperar a la foto.
        val avatar = data["senderPhoto"]?.takeIf { it.isNotBlank() }?.let { runBlocking { loadAvatar(it) } }

        NotificationHelper.showMessage(
            context = this,
            chatId = chatId,
            title = data["title"].orEmpty().ifBlank { senderName },
            senderName = senderName,
            body = body,
            isGroup = data["isGroup"] == "1",
            avatar = avatar,
        )
    }

    private suspend fun loadAvatar(url: String): Bitmap? {
        val request = ImageRequest.Builder(this)
            .data(url)
            .size(192)
            .transformations(CircleCropTransformation())
            .allowHardware(false)
            .build()
        return (imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
    }
}
