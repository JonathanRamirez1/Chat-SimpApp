package com.jonathan.loginfuturo.services

import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.jonathan.loginfuturo.Constants
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.chanel.NotificationHelper
import com.jonathan.loginfuturo.model.Message
import com.jonathan.loginfuturo.providers.MessageProvider
import com.jonathan.loginfuturo.receivers.MessageReceiver
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.lang.Exception
import java.util.*

class MyFirebaseMessagingClient: FirebaseMessagingService() {

    companion object {
        const val NOTIFICATION_REPLY = "NotificationReply"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data: Map<String, String> = remoteMessage.data
        val title: String? = data["title"]
        val body: String? = data["body"]
        if (title != null) {
            if (title == "NEW MESSAGE") {
                showNotificationMessage(data)
            } else {
                showNotification(title, body)
            }
        }
    }

    private fun showNotification(title: String?, body: String?) {
        val notificationHelper = NotificationHelper(baseContext)
        val builder: NotificationCompat.Builder = notificationHelper.getNotification(title, body)
        val random = Random()
        val n: Int = random.nextInt(10000)
        notificationHelper.getManager().notify(n, builder.build())
    }

    private fun showNotificationMessage(data: Map<String, String>) {
        val imageEmisor = data["imageEmisor"].toString()
        val imageReceptor = data["imageReceptor"].toString()
        Log.d("ENTRO", "NEW MESSAGE")
        getImageEmisor(data, imageEmisor, imageReceptor)
    }

    private fun getImageEmisor(data: Map<String, String>, imageEmisor: String?, imageReceptor: String) {
        Handler(Looper.getMainLooper())
            .post {
                Picasso.get()
                    .load(imageEmisor)
                    .resize(100, 100)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(object : Target {
                        override fun onBitmapLoaded(bitmapEmisor: Bitmap?, from: LoadedFrom?) {
                            getImageReceptor(data, imageReceptor, bitmapEmisor)
                        }

                        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                            getImageReceptor(data, imageReceptor, null)
                        }

                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
                    })
            }
    }

    private fun getImageReceptor(data: Map<String, String>, imageReceptor: String, bitmapEmisor: Bitmap?) {
        Picasso.get()
            .load(imageReceptor)
            .resize(100, 100)
            .centerCrop()
            .transform(CircleTransform())
            .into(object : Target {
                override fun onBitmapLoaded(bitmapReceptor: Bitmap, from: LoadedFrom?) {
                    if (bitmapEmisor != null) {
                        hola(data, bitmapEmisor, bitmapReceptor)
                    }
                }
                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    if (bitmapEmisor != null) {
                        hola(data, bitmapEmisor, null)
                    }
                }
                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
            })
    }

    private fun notifyMessage(data: Map<String, String>, bitmapEmisor: Bitmap, bitmapReceptor: Bitmap?) {
        val usernameEmisor = data["usernameEmisor"].toString()
        val usernameReceptor = data["usernameReceptor"].toString()
        val lastMessage = data["lastMessage"].toString()
        val messagesJSON = data["messages"]
        val idEmisor = data["idEmisor"]
        val idReceptor = data["idReceptor"]
        val idChat = data["idChat"]
        val idNotification: Int = data["idNotification"]!!.toInt()
        val imageEmisor = data["imageEmisor"].toString()
        val imageReceptor = data["imageReceptor"].toString()

        val intent = Intent(this, MessageReceiver::class.java)
        intent.putExtra("idEmisor", idEmisor)
        intent.putExtra("idReceptor", idReceptor)
        intent.putExtra("idChat", idChat)
        intent.putExtra("idNotification", idNotification)
        intent.putExtra("usernameEmisor", usernameEmisor)
        intent.putExtra("usernameReceptor", usernameReceptor)
        intent.putExtra("imageEmisor", imageEmisor)
        intent.putExtra("imageReceptor", imageReceptor)
        val pendingIntent = PendingIntent.getBroadcast(this, Constants.REQUEST_CODE_NOTIFY_MESSAGE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val remoteInput = RemoteInput.Builder(NOTIFICATION_REPLY).setLabel(resources.getString(R.string.your_message)).build()

        val action = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            resources.getString(R.string.reply_message),
            pendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        val gson = Gson()
        val messages = gson.fromJson(messagesJSON, Array<Message>::class.java)
        val notificationHelper = NotificationHelper(baseContext)
        val builder = notificationHelper.getNotificationMessage(
            messages,
            usernameEmisor,
            usernameReceptor,
            lastMessage,
            bitmapEmisor,
            bitmapReceptor,
            action)
        notificationHelper.getManager().notify(idNotification, builder.build())
    }

    private fun notifyMessageEmisor(data: Map<String, String>, bitmapEmisor: Bitmap) {
        val usernameEmisor = data["usernameEmisor"].toString()
        val messagesJSON = data["messages"]
        val idEmisor = data["idEmisor"]
        val idChat = data["idChat"]
        val idNotification: Int = data["idNotification"]!!.toInt()
        val imageEmisor = data["imageEmisor"].toString()

        val intent = Intent(this, MessageReceiver::class.java)
        intent.putExtra("idEmisor", idEmisor)
        intent.putExtra("idChat", idChat)
        intent.putExtra("idNotification", idNotification)
        intent.putExtra("usernameEmisor", usernameEmisor)
        intent.putExtra("imageEmisor", imageEmisor)
        val pendingIntent = PendingIntent.getBroadcast(this, Constants.REQUEST_CODE_NOTIFY_MESSAGE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val remoteInput = RemoteInput.Builder(NOTIFICATION_REPLY).setLabel(resources.getString(R.string.your_message)).build()

        val action = NotificationCompat.Action.Builder(
            R.mipmap.ic_launcher,
            resources.getString(R.string.reply_message),
            pendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        val gson = Gson()
        val messages = gson.fromJson(messagesJSON, Array<Message>::class.java)
        val notificationHelper = NotificationHelper(baseContext)
        val builder = notificationHelper.getNotificationMessageEmisor(
            messages,
            usernameEmisor,
            bitmapEmisor,
            action)
        notificationHelper.getManager().notify(idNotification, builder.build())
    }

    private fun hola(data: Map<String, String>, bitmapEmisor: Bitmap, bitmapReceptor: Bitmap?) {
        val messageProvider = MessageProvider()
        val idChat: String = ""
        val idEmisor: String = ""
        messageProvider.getLastMessageEmisor(idChat, idEmisor).get()
            .addOnSuccessListener { querySnapshot ->
                val size: Int = querySnapshot.size()
                if (size > 0) {
                    notifyMessage(data, bitmapEmisor, bitmapReceptor)
                } else {
                    notifyMessageEmisor(data, bitmapEmisor)
                }
            }
    }
}