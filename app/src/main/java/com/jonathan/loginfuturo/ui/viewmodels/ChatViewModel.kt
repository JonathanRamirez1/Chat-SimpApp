package com.jonathan.loginfuturo.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.jonathan.loginfuturo.core.CircleTransform
import com.jonathan.loginfuturo.core.SingleLiveEvent
import com.jonathan.loginfuturo.data.model.ChatModel
import com.jonathan.loginfuturo.data.model.FCMBody
import com.jonathan.loginfuturo.data.model.FCMResponse
import com.jonathan.loginfuturo.data.model.MessageModel
import com.jonathan.loginfuturo.data.model.providers.*
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class ChatViewModel : ViewModel() {

    //Providers
    val messageProvider = MessageProvider()
    private val authProvider = AuthProvider()
    private val chatProvider = ChatProvider()
    private val tokenProvider = TokenProvider()
    private val userProvider = UserProvider()
    private val notificationProvider = NotificationProvider()

    //Models
    val chatModel = ChatModel()
    val messageModel = MessageModel()

    var idUserEmisor: String = ""
    var idUserReceptor: String = ""
    var idChat: String = ""
    var lastConnect: Long = 0

    private var usernameEmisor: String = ""
    private var usernameReceptor: String = ""
    private var imageEmisor: String = ""
    private var imageReceptor: String = ""
    private var idNotificationChat: Long? = 0

    private var lastMessageEmisorRegistration: ListenerRegistration? = null
    private var userInfoRegistration: ListenerRegistration? = null

    private val _isNotifyMessage = MutableLiveData<Boolean>()
    val isNotifyMessage: LiveData<Boolean> = _isNotifyMessage

    private val _showToast: SingleLiveEvent<String> = SingleLiveEvent()
    val showToast: LiveData<String> = _showToast

    private val _editTextMessage = MutableLiveData<String>()
    val editTextMessage: LiveData<String> = _editTextMessage

    private val _usernameChat = MutableLiveData<String>()
    val usernameChat: LiveData<String> = _usernameChat

    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline

    private val _imageChat = MutableLiveData<RequestCreator>()
    val imageChat: LiveData<RequestCreator> = _imageChat

    fun getUserInfo() {
        var idUserInfo = ""
        idUserInfo = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        userInfoRegistration = userProvider.getUserRealTime(idUserInfo)
            .addSnapshotListener { documentSnapshot, _ ->
                if (documentSnapshot != null) {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("username")) {
                            usernameReceptor = documentSnapshot.getString("username").toString()
                            _usernameChat.value = usernameReceptor
                        }
                        if (documentSnapshot.contains("online")) {
                            val online: Boolean? = documentSnapshot.getBoolean("online")
                            if (online == true) {
                                _isOnline.value = true
                            } else if (documentSnapshot.contains("lastConnect")) {
                                lastConnect = documentSnapshot.getLong("lastConnect")!!.toLong()
                                _isOnline.value = false
                            }
                        }
                        if (documentSnapshot.contains("photo")) {
                            imageReceptor = documentSnapshot.getString("photo").toString()
                            if (imageReceptor.isNotEmpty()) {
                                _imageChat.value = Picasso.get()
                                    .load(imageReceptor)
                                    .resize(100, 100)
                                    .centerCrop()
                                    .transform(CircleTransform())
                            }
                        }
                    }
                }
            }
    }

    fun checkChatExist() {
        chatProvider.getOneToOneChat(idUserEmisor, idUserReceptor).get()
            .addOnSuccessListener { querySnapshot ->
                val size: Int = querySnapshot.size()
                if (size == 0) {
                    createChat()
                } else {
                    idChat = querySnapshot.documents[0].id
                    idNotificationChat = querySnapshot.documents[0].getLong("idNotification")
                    updateViewed()
                }
            }
    }

    private fun createChat() {
        val random = Random()
        val n: Int = random.nextInt(1000000)
        chatModel.setIdEmisor(idUserEmisor)
        chatModel.setIdReceptor(idUserReceptor)
        chatModel.setWritting(false)
        chatModel.setTimeStamp(Date())
        chatModel.setIdNotification(n)
        idNotificationChat = n.toLong()

        val ids: ArrayList<String> = ArrayList()
        ids.add(idUserEmisor)
        ids.add(idUserReceptor)
        chatModel.setIds(ids)
        chatProvider.createCollectionUsers(chatModel)
    }

    fun updateViewed() {
        var idUserEmisorForViewed = ""
        idUserEmisorForViewed = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        messageProvider.getMessageByChatEmisor(idChat, idUserEmisorForViewed).get()
            .addOnSuccessListener { querySnapshot ->
                for (document: DocumentSnapshot in querySnapshot.documents) {
                    messageProvider.updateViewed(document.id, true)
                }
            }
    }

    fun message(messageText: String) {
        if (messageText.isNotEmpty()) {
            messageModel.setIdChat(idChat)
            if (authProvider.getUid() == idUserEmisor) {
                messageModel.setIdEmisor(idUserEmisor)
                messageModel.setIdReceptor(idUserReceptor)
            } else {
                messageModel.setIdEmisor(idUserReceptor)
                messageModel.setIdReceptor(idUserEmisor)
            }
            messageModel.setTimeStamp(Date().time)
            messageModel.setViewed(false)
            messageModel.setIdChat(idChat)
            messageModel.setMessage(messageText)

            messageProvider.create(messageModel).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _editTextMessage.value = ""
                    _isNotifyMessage.value = true
                    getToken()
                } else {
                    _showToast.value = "Message Error, try again!"
                }
            }
        }
    }

    private fun getToken() {
        var idUserForToken = ""
        idUserForToken = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        tokenProvider.getToken(idUserForToken).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot != null) {
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        val token: String = documentSnapshot.getString("token").toString()
                        getLastThreeMessages(token)
                    }
                } else {
                    _showToast.value = "El token de notificaciones del usuario no existe"
                }
            }
        }
    }

    private fun getLastThreeMessages(token: String) {
        messageProvider.getLastThreeMessageByChatEmisor(idChat, authProvider.getUid()).get()
            .addOnSuccessListener { querySnapshot ->
                val messageArrayList = ArrayList<MessageModel>()

                for (documentSnapshot in querySnapshot.documents) {
                    if (documentSnapshot.exists()) {
                        val messageModel1: MessageModel? =
                            documentSnapshot.toObject(MessageModel::class.java)
                        messageArrayList.add(messageModel1!!)
                    }
                }
                if (messageArrayList.size == 0) {
                    messageArrayList.add(messageModel)
                }
                messageArrayList.reverse()

                val gson = Gson()
                val messages = gson.toJson(messageArrayList)
                sendNotification(token, messages, messageModel)
            }
    }

    private fun sendNotification(token: String, messages: String, messageModel: MessageModel) {
        var lastMessage = ""
        val data: MutableMap<String, String> = HashMap()
        data["title"] = "NEW MESSAGE"
        data["body"] = messageModel.getMessage()
        data["idNotification"] = idNotificationChat.toString()
        data["messages"] = messages
        data["usernameEmisor"] = usernameEmisor.uppercase(Locale.getDefault())
        data["usernameReceptor"] = usernameReceptor.uppercase(Locale.getDefault())
        data["imageEmisor"] = imageEmisor
        data["imageReceptor"] = imageReceptor
        data["idEmisor"] = messageModel.getIdEmisor()
        data["idReceptor"] = messageModel.getIdReceptor()
        data["idChat"] = messageModel.getIdChat()

        if (imageEmisor.isEmpty()) {
            imageEmisor = "IMAGEN_NO_VALIDA"
        }
        if (imageReceptor.isEmpty()) {
            imageReceptor = "IMAGEN_NO_VALIDA"
        }

        var idUserEmisorForNotification = ""
        idUserEmisorForNotification = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        lastMessageEmisorRegistration =
            messageProvider.getLastMessageEmisor(idChat, idUserEmisorForNotification)
                .addSnapshotListener { querySnapshot, _ ->
                    if (querySnapshot != null) {
                        val size: Int = querySnapshot.size()
                        if (size > 0) {
                            lastMessage = querySnapshot.documents[0].getString("message").toString()
                            data["lastMessage"] = lastMessage
                        } else {
                            data["lastMessage"] = "No messages yet"
                        }
                        val body = FCMBody(token, "high", "4500s", data)
                        notificationProvider.sendNotification(body)?.enqueue(object :
                            Callback<FCMResponse?> {
                            override fun onResponse(
                                call: Call<FCMResponse?>?,
                                response: Response<FCMResponse?>
                            ) {
                                if (response.body() != null) {
                                    if (response.body()!!.getSuccess() == 1) {
                                        //TODO implement
                                    } else {
                                        _showToast.value = "La notificacion no se pudo enviar"
                                    }
                                } else {
                                    _showToast.value = "La notificacion no se pudo enviar"
                                }
                            }

                            override fun onFailure(call: Call<FCMResponse?>?, t: Throwable?) {}
                        })
                    }
                }
    }

    fun getInfoUserForNotifications() {
        userProvider.getUser(authProvider.getUid()).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("username")) {
                    usernameEmisor = documentSnapshot.getString("username").toString()
                }
                if (documentSnapshot.contains("photo")) {
                    imageEmisor = documentSnapshot.getString("photo").toString()
                }
            }
        }
    }

    fun getUserInfoListener(): ListenerRegistration? {
        return userInfoRegistration
    }

    fun getLastMessageListener(): ListenerRegistration? {
        return lastMessageEmisorRegistration
    }

}