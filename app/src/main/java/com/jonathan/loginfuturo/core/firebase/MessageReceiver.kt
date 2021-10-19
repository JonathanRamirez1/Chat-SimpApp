package com.jonathan.loginfuturo.core.firebase

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.RemoteInput
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.jonathan.loginfuturo.data.model.FCMBody
import com.jonathan.loginfuturo.data.model.FCMResponse
import com.jonathan.loginfuturo.data.model.MessageModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.MessageProvider
import com.jonathan.loginfuturo.data.model.providers.NotificationProvider
import com.jonathan.loginfuturo.data.model.providers.TokenProvider
import com.jonathan.loginfuturo.data.network.MyFirebaseMessagingClient.Companion.NOTIFICATION_REPLY
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class MessageReceiver: BroadcastReceiver() {

    private lateinit var tokenProvider: TokenProvider
    private lateinit var notificationProvider: NotificationProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var messageProvider: MessageProvider
    private lateinit var messageModel: MessageModel

    private var idEmisor: String = ""
    private var idReceptor: String = ""
    private var idChat: String = ""
    private var usernameEmisor: String = ""
    private var usernameReceptor: String = ""
    private var imageEmisor: String = ""
    private var imageReceptor: String = ""
    private var idNotification: Int = 0

    private var lastMessageEmisorRegistration: ListenerRegistration? = null

    override fun onReceive(context: Context, intent: Intent) {
        idEmisor = intent.getStringExtra("idEmisor").toString()
        idReceptor = intent.getStringExtra("idReceptor").toString()
        idChat = intent.getStringExtra("idChat").toString()
        usernameEmisor = intent.getStringExtra("usernameEmisor").toString()
        usernameReceptor = intent.getStringExtra("usernameReceptor").toString()
        imageEmisor = intent.getStringExtra("imageEmisor").toString()
        imageReceptor = intent.getStringExtra("imageReceptor").toString()
        idNotification = Integer.parseInt(intent.extras?.getInt("idNotification").toString())

        tokenProvider = TokenProvider()
        notificationProvider = NotificationProvider()
        authProvider = AuthProvider()
        messageProvider = MessageProvider()
        messageModel = MessageModel()

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(idNotification)

        val message: String = getMessageNotification(intent).toString()
        sendMessage(message)
    }

    private fun sendMessage(messageNotification: String) {
        messageModel.setIdChat(idChat)
        messageModel.setIdEmisor(idReceptor)
        messageModel.setIdReceptor(idEmisor)
        messageModel.setTimeStamp(Date().time)
        messageModel.setViewed(false)
        messageModel.setIdChat(idChat)
        messageModel.setMessage(messageNotification)

        val messageProvider = MessageProvider()
        messageProvider.create(messageModel).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                getToken()
            }
        }
    }

    private fun getToken() {
        tokenProvider.getToken(idEmisor).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot != null) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        val token: String = documentSnapshot.getString("token").toString()
                        val gson = Gson()
                        val messageModelArrayList: ArrayList<MessageModel> = ArrayList()
                        messageModelArrayList.add(messageModel)
                        val messages = gson.toJson(messageModelArrayList)
                        sendNotification(token, messages)
                    }
                }
            }
        }
    }

    private fun sendNotification(token: String, messages: String) {
        val data: MutableMap<String, String> = HashMap()
        data["title"] = "NEW MESSAGE"
        data["body"] = messageModel.getMessage()
        data["idNotification"] = idNotification.toString()
        data["messages"] = messages
        data["usernameEmisor"] = usernameReceptor.uppercase(Locale.getDefault())
        data["usernameReceptor"] = usernameEmisor.uppercase(Locale.getDefault())
        data["imageEmisor"] = imageReceptor
        data["imageReceptor"] = imageEmisor
        data["idEmisor"] = messageModel.getIdEmisor()
        data["idReceptor"] = messageModel.getIdReceptor()
        data["idChat"] = messageModel.getIdChat()

        var idUserEmisorForNotification = ""
        idUserEmisorForNotification = if (authProvider.getUid() == idEmisor) {
            idReceptor
        } else {
            idEmisor
        }
       lastMessageEmisorRegistration =  messageProvider.getLastMessageEmisor(idChat, idUserEmisorForNotification).addSnapshotListener { querySnapshot, _ ->
           if (querySnapshot != null) {
               val size: Int = querySnapshot.size()
               var lastMessage = ""
               if (size > 0) {
                   lastMessage = querySnapshot.documents[0].getString("message").toString()
                   data["lastMessage"] = lastMessage
               }
               val body = FCMBody(token, "high", "4500s", data)
               notificationProvider.sendNotification(body)?.enqueue(object : Callback<FCMResponse?> {
                       override fun onResponse(call: Call<FCMResponse?>?, response: Response<FCMResponse?>) {}
                       override fun onFailure(call: Call<FCMResponse?>?, t: Throwable) {
                           Log.d("ERROR", "El error fue: " + t.message)
                       }
                   })
           }
       }
    }

    private fun getMessageNotification(intent: Intent): CharSequence? {
        val remoteInput: Bundle? = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput != null) {
            return remoteInput.getCharSequence(NOTIFICATION_REPLY)
        }
        return null
    }

    fun getListenerLastMessageEmisor(): ListenerRegistration? {
        return lastMessageEmisorRegistration
    }
}