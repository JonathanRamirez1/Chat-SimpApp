package com.jonathan.chatsimpapp.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.jonathan.chatsimpapp.MainActivity
import com.jonathan.chatsimpapp.R

object NotificationHelper {
    const val CHANNEL_MESSAGES = "messages"
    const val EXTRA_CHAT_ID = "chatId"

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_MESSAGES,
            context.getString(R.string.notification_channel_messages),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notification_channel_messages_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showMessage(
        context: Context,
        chatId: String,
        title: String,
        senderName: String,
        body: String,
        isGroup: Boolean,
        avatar: Bitmap?,
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val manager = NotificationManagerCompat.from(context)
        val notificationId = chatId.hashCode()

        val sender = Person.Builder()
            .setName(senderName)
            .apply { avatar?.let { setIcon(IconCompat.createWithBitmap(it)) } }
            .build()
        val me = Person.Builder().setName(context.getString(R.string.you)).build()

        // Añade el mensaje a la conversación que ya estaba en la notificación (si había una).
        val existing = manager.activeNotifications.firstOrNull { it.id == notificationId }?.notification
        val style = existing
            ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
            ?: NotificationCompat.MessagingStyle(me)
        style.conversationTitle = if (isGroup) title else null
        style.isGroupConversation = isGroup
        style.addMessage(body, System.currentTimeMillis(), sender)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_CHAT_ID, chatId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.brand))
            .setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }

    fun cancel(context: Context, chatId: String) {
        NotificationManagerCompat.from(context).cancel(chatId.hashCode())
    }
}
