package com.jonathan.chatsimpapp.ui.viewmodels

import android.app.Application
import android.content.ClipDescription
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.jonathan.chatsimpapp.core.SingleLiveEvent
import com.jonathan.chatsimpapp.core.objects.Constants
import com.jonathan.chatsimpapp.core.objects.ConvertUriToFile
import com.jonathan.chatsimpapp.data.model.ChatGroupModel
import com.jonathan.chatsimpapp.data.model.FCMBody
import com.jonathan.chatsimpapp.data.model.FCMResponse
import com.jonathan.chatsimpapp.data.model.MessageModel
import com.jonathan.chatsimpapp.data.model.providers.*
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class ChatGroupViewModel(application: Application): AndroidViewModel(application) {

    //Providers
    private val chatGroupProvider = ChatGroupProvider()
    private val authProvider = AuthProvider()
    private val messageProvider = MessageProvider()
    private val tokenProvider = TokenProvider()
    private val notificationProvider = NotificationProvider()
    private val imageProvider = ImageProvider()

    //Models
    val chatGroupModel = ChatGroupModel()
    val messageModel = MessageModel()

    var ids: ArrayList<String> = ArrayList()
    var idGroup: String = ""
    var idAdmin: String = ""
    var email: String = ""
    var idUserEmisor: String = ""
    var idUserReceptor: String = ""

    private var usernameEmisor: String = ""
    private var usernameReceptor: String = ""
    private var imageEmisor: String = ""
    private var imageReceptor: String = ""
    private var groupGalleryPath: String = ""
    private var groupCameraPath: String = ""
    private var absoluteGroupGalleryPath: String = ""
    private var absoluteGroupCameraPath: String = ""
    private var idNotificationChatGroup: Long? = 0

    private var lastMessageEmisorRegistration: ListenerRegistration? = null
    private var fileGroupGallery: File? = null
    private var fileGroupCamera: File? = null

    private val _isNotifyMessage = MutableLiveData<Boolean>()
    val isNotifyMessage: LiveData<Boolean> = _isNotifyMessage

    private val _showToast: SingleLiveEvent<String> = SingleLiveEvent()
    val showToast: LiveData<String> = _showToast

    private val _editTextMessage = MutableLiveData<String>()
    val editTextMessage: LiveData<String> = _editTextMessage

    fun checkChatGroupExist() {
        chatGroupProvider.getChatGroup(ids).get()
            .addOnSuccessListener { querySnapshot ->
                val size: Int = querySnapshot.size()
                if (size == 0) {
                    createChatGroup()
                } else {
                    idGroup = querySnapshot.documents[0].id
                    idNotificationChatGroup = querySnapshot.documents[0].getLong("idNotification")
                    //TODO implementar updateViewed()
                }
            }
    }

    private fun createChatGroup() {
        val random = Random()
        val sizeIdNotification: Int = random.nextInt(1000000)
        chatGroupModel.setIdGroup(idGroup)
        chatGroupModel.setIdAdmin(idAdmin)
        chatGroupModel.setIdNotification(sizeIdNotification)
        chatGroupModel.setTimeStamp(Date())
        chatGroupModel.setEmail(email)
        idNotificationChatGroup = sizeIdNotification.toLong()

        val idUsers: ArrayList<String> = ArrayList()
        for (i in 0..idUsers.size) {
            idUsers.add(ids.toString())
            chatGroupModel.setIdUsers(idUsers)
            Log.e("Error", "Aqui fue el error: $idUsers")
        }
        chatGroupProvider.createChatGroup(chatGroupModel) //TODO PROBAR PONERLO FUERA DEL FOR
        Log.e("Error2", "Aqui fue el error: $idUsers")
    }

    fun message(messageText: String) {
        if (messageText.isNotEmpty()) {
            messageModel.setIdChat(idGroup)
            if (authProvider.getUid() == idUserEmisor) {
                messageModel.setIdEmisor(idUserEmisor)
                messageModel.setIdReceptor(idUserReceptor)
            } else {
                messageModel.setIdEmisor(idUserReceptor)
                messageModel.setIdReceptor(idUserEmisor)
            }
            messageModel.setTimeStamp(Date().time)
            messageModel.setViewed(false)
            messageModel.setIdChat(idGroup)
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
        messageProvider.getLastThreeMessageByChatEmisor(idGroup, authProvider.getUid()).get()
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
        data["idNotification"] = idNotificationChatGroup.toString()
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
            messageProvider.getLastMessageEmisor(idGroup, idUserEmisorForNotification)
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

    fun validDataGroup(nameGroup: String, description: String) {
        if (nameGroup.isNotEmpty() && description.isNotEmpty()) {

        }
    }

    private fun saveImageGroupStorage(nameGroup: String, description: String) {
        imageProvider.getStorage().downloadUrl.addOnSuccessListener { uriGroup ->
            val photoGroup: String = uriGroup.toString()
            chatGroupModel.setNameGroup(nameGroup)
            chatGroupModel.setDescription(description)
            chatGroupModel.setPhotoGroup(photoGroup)
            chatGroupProvider.createChatGroup(chatGroupModel)
        }
    }

    /**Crea un archivo cuando se toma foto desde la camara**/
    fun groupFile(requestCode: Int, cameraFile: File): File {
        if (requestCode == Constants.REQUEST_CODE_CAMERA_GROUP) {
            groupGalleryPath = "file:" +cameraFile.absoluteFile
            absoluteGroupGalleryPath = cameraFile.absolutePath
        }
        return cameraFile
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?, imageGroup: CircleImageView) {
        /**Selecciona imagen de la galeria**/
        if (requestCode == Constants.REQUEST_CODE_GALLERY_GROUP && resultCode == AppCompatActivity.RESULT_OK) {
            try {
                fileGroupCamera = null
                fileGroupGallery = data?.data?.let { ConvertUriToFile.from(getApplication(), it) }
                imageGroup.setImageBitmap(BitmapFactory.decodeFile(fileGroupGallery?.absolutePath))
            } catch (exception: Exception) {
                Log.d("ERROR", "There was an error" + exception.message)
            }
        }
        /**Toma una foto de la camara**/
        if (requestCode == Constants.REQUEST_CODE_CAMERA_GROUP && resultCode == AppCompatActivity.RESULT_OK) {
            fileGroupGallery = null
            fileGroupCamera = File(absoluteGroupGalleryPath)
            Picasso.get().load(groupGalleryPath).into(imageGroup)
        }
    }
}