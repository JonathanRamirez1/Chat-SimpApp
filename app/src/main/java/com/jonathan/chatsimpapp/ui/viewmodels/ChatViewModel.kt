package com.jonathan.chatsimpapp.ui.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.jonathan.chatsimpapp.core.objects.OfflinePersistence
import com.jonathan.chatsimpapp.core.CircleTransform
import com.jonathan.chatsimpapp.core.SingleLiveEvent
import com.jonathan.chatsimpapp.data.model.*
import com.jonathan.chatsimpapp.data.model.providers.*
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
    private val messageModel = MessageModel()

    var idUserEmisor: String = ""
    var idUserReceptor: String = ""
    var idUserWriting: String = ""
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

    private val _online: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val online: LiveData<Boolean> = _online

    private val _isOffline: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val isOffline: LiveData<Boolean> = _isOffline

    private val _imageChat = MutableLiveData<RequestCreator>()
    val imageChat: LiveData<RequestCreator> = _imageChat

    private val _isWriting = MutableLiveData<Boolean>()
    val isWriting: LiveData<Boolean> = _isWriting

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

    /**Comprueba si un chat existe, sino existe se agrega a firestore y se muestra en el RoomsFragment**/
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
        chatModel.setTimeStamp(Date())
        chatModel.setIdNotification(n)
        idNotificationChat = n.toLong()

        val ids: ArrayList<String> = ArrayList()
        ids.add(idUserEmisor)
        ids.add(idUserReceptor)
        chatModel.setIds(ids)
        chatProvider.createCollectionUsers(chatModel)
    }

    fun createChatInRealTimeDB() {
        idUserWriting = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        chatProvider.createNodeUsers(chatModel, idUserWriting)
    }

    fun updateWritingUser(writing: String) {
        idUserWriting = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        chatProvider.updateWriting(idChat, idUserWriting, writing)
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

    /**Obtiene el estado del usuario en Firebase RealTime Database**/
    fun getStateUser() {
        var idUserRealTime = ""
        idUserRealTime = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        OfflinePersistence.getUserRealTimeDatabase(idUserRealTime)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        if (dataSnapshot.child("userState").hasChild("online")) {
                            val online: String = dataSnapshot.child("userState").child("online").value.toString()
                            if (online == "true") {
                                _online.value = true
                            } else if (dataSnapshot.child("userState").hasChild("lastConnect")) {
                                lastConnect = dataSnapshot.child("userState").child("lastConnect").value.toString().toLong()
                                _online.value = false
                            }
                            if (dataSnapshot.child("userState").hasChild("offline")) {
                                val offline: String = dataSnapshot.child("userState").child("offline").value.toString()
                                if (offline == "true") {
                                    _isOffline.value = true
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Cancel", "Se cancelo la operacion{$error}")
                }
            })
    }

    fun checkIsWriting() {
        idUserWriting = if (idUserWriting == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        chatProvider.getWritingRealTimeDB(idChat, authProvider.getUid())
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        if (dataSnapshot.hasChild("writing")) {
                            val writing: String = dataSnapshot.child("writing").value.toString()
                            if (writing == "true") {
                                _isWriting.value = true
                            } else if (writing == "false") {
                                _isWriting.value = false
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CancelWriting", "Se cancelo la operacion{$error}")
                }
            })
    }

    /**Si el usuario se desconecta (No Internet) del servidor, se llama a este metodo, que pondra el estado "offline" del usuario en "true"**/
    fun setOnDisconnect(): Task<Void> {
        return OfflinePersistence.getUserRealTimeDatabase(authProvider.getUid())
            .child("userState")
            .child("offline")
            .onDisconnect()
            .setValue("true")
    }

    fun getUserInfoListener(): ListenerRegistration? {
        return userInfoRegistration
    }

    fun getLastMessageListener(): ListenerRegistration? {
        return lastMessageEmisorRegistration
    }
}