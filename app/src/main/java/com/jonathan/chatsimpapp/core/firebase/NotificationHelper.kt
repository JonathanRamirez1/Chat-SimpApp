package com.jonathan.chatsimpapp.core.firebase

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.data.model.MessageModel
import java.util.*

class NotificationHelper(context: Context?) : ContextWrapper(context) {

    private var notificationManager: NotificationManager? = null

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createChannels() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.enableLights(true)
        notificationChannel.enableVibration(true)
        notificationChannel.lightColor = Color.GRAY
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getManager().createNotificationChannel(notificationChannel)
    }

    fun getManager(): NotificationManager {
        if (notificationManager == null) {
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
        return notificationManager as NotificationManager
    }

    fun getNotification(title: String?, body: String?): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setColor(Color.GRAY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
    }

    fun getNotificationMessage(messageModels: Array<MessageModel>,
                               usernameEmisor: String,
                               usernameReceptor: String,
                               lastMessage: String,
                               bitmapEmisor: Bitmap?,
                               bitmapReceptor: Bitmap?,
                               action: NotificationCompat.Action
    ): NotificationCompat.Builder {
        var person1: Person? = null
        person1 = if (bitmapReceptor == null) {
            Person.Builder()
                .setName(usernameReceptor)
                .setIcon(IconCompat.createWithResource(applicationContext, R.drawable.person))
                .build()
        } else {
            Person.Builder()
                .setName(usernameReceptor)
                .setIcon(IconCompat.createWithBitmap(bitmapReceptor))
                .build()
        }

        var person2: Person? = null
        person2 = if (bitmapEmisor == null) {
            Person.Builder()
                .setName(usernameEmisor)
                .setIcon(IconCompat.createWithResource(applicationContext, R.drawable.person))
                .build()
        } else {
            Person.Builder()
                .setName(usernameEmisor)
                .setIcon(IconCompat.createWithBitmap(bitmapEmisor))
                .build()
        }

        val messagingStyle: NotificationCompat.MessagingStyle = NotificationCompat.MessagingStyle(person1)
        val message1: NotificationCompat.MessagingStyle.Message = NotificationCompat.MessagingStyle.Message(
            lastMessage,
            Date().time,
            person1)
        messagingStyle.addMessage(message1)

        for (messageModel: MessageModel in messageModels) {
            val message2: NotificationCompat.MessagingStyle.Message = NotificationCompat.MessagingStyle.Message(
                messageModel.getMessage(),
                messageModel.getTimeStamp(),
                person2)
            messagingStyle.addMessage(message2)
        }

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(messagingStyle)
            .addAction(action)
    }

    companion object {
        private const val CHANNEL_ID = "com.jonathan.loginfuturo"
        private const val CHANNEL_NAME = "LoginFuturo"
    }
}