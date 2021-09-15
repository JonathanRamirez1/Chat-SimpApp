package com.jonathan.loginfuturo.view.fragments

import android.content.Context.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.databinding.FragmentChatBinding
import com.jonathan.loginfuturo.model.Message
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.model.ChatModel
import com.jonathan.loginfuturo.view.adapters.MessageAdapter
import java.util.*
import kotlin.collections.ArrayList
import com.google.firebase.firestore.DocumentSnapshot
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.Utils.RelativeTime
import com.jonathan.loginfuturo.Utils.ViewedMessageHelper
import com.jonathan.loginfuturo.Utils.WrapContentLinearLayoutManager
import com.squareup.picasso.Picasso
import android.net.ConnectivityManager
import com.google.gson.Gson
import com.jonathan.loginfuturo.model.FCMBody
import com.jonathan.loginfuturo.model.FCMResponse
import com.jonathan.loginfuturo.providers.*
import retrofit2.Callback
import kotlin.collections.HashMap
import retrofit2.Call
import retrofit2.Response

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var navController: NavController
    private lateinit var chatProvider: ChatProvider
    private lateinit var message: Message
    private lateinit var messageProvider: MessageProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider
    private lateinit var tokenProvider: TokenProvider
    private lateinit var notificationProvider: NotificationProvider
    private lateinit var wrapContentLinearLayoutManager: WrapContentLinearLayoutManager

    private var messageAdapter: MessageAdapter? = null
    private var chatRegistration: ListenerRegistration? = null

    private var idUserEmisor: String = ""
    private var idUserReceptor: String = ""
    private var idChat: String = "" //TODO ALGUN ERROR QUITAR EL STRING
    private var usernameEmisor: String = ""
    private var usernameReceptor: String = ""
    private var imageEmisor: String = ""
    private var imageReceptor: String = ""
    private var idNotificationChat: Long? = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)

        chatProvider = ChatProvider()
        messageProvider = MessageProvider()
        authProvider = AuthProvider()
        userProvider = UserProvider()
        tokenProvider = TokenProvider()
        message = Message()
        notificationProvider = NotificationProvider()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getDataUsers()
        getUsernameForNotifications()
        checkChatExist()
        sendMessage()
        launchRoomsFragment(view)
        getUserInfo()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStart() {
        super.onStart()
        if (messageAdapter != null) {
            messageAdapter?.startListening()
        }
         ViewedMessageHelper.updateState(true, requireContext())
        //getConnectivity()
    }

    override fun onPause() {
        super.onPause()
        ViewedMessageHelper.updateState(false, requireContext())
    }

    override fun onStop() {
        super.onStop()
        messageAdapter?.stopListening()
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (chatRegistration != null) {
            chatRegistration?.remove()
        }
    }

     private fun getDataUsers() {
         wrapContentLinearLayoutManager = WrapContentLinearLayoutManager(context)
         wrapContentLinearLayoutManager.stackFromEnd = true
         binding.recyclerViewChat.layoutManager = wrapContentLinearLayoutManager
        idUserEmisor = arguments?.getString("idEmisor").toString()
        idUserReceptor = arguments?.getString("idReceptor").toString()
        idChat = arguments?.getString("idChat").toString()
    }

    private fun checkChatExist() {
        chatProvider.getOneToOneChat(idUserEmisor, idUserReceptor).get()
            .addOnSuccessListener { querySnapshot ->
                val size: Int = querySnapshot.size()
                if (size == 0) {
                    createChat() //TODO CORREGIR ERROR QUE SE AGREGA EL CHAT SIN ESCRIBIR NINGUN MENSAJE
                } else {
                    idChat = querySnapshot.documents[0].id  //TODO 7 11
                    idNotificationChat = querySnapshot.documents[0].getLong("idNotification")
                    getAllMessagesUser()
                    updateViewed()
                }
            }
    }

    private fun createChat() {
        val chat = ChatModel()
        val random = Random()
        val n: Int = random.nextInt(1000000)
        chat.setIdEmisor(idUserEmisor)
        chat.setIdReceptor(idUserReceptor)
        chat.setWritting(false)
        chat.setTimeStamp(Date())
        chat.setId(idUserEmisor + idUserReceptor)
        chat.setIdNotification(n)
        idNotificationChat = n.toLong()

        val ids: ArrayList<String> = ArrayList()
        ids.add(idUserEmisor)
        ids.add(idUserReceptor)
        chat.setIds(ids)
        chatProvider.createCollectionUsers(chat)
        idChat = chat.getId()
        getAllMessagesUser()
    }

    private fun getAllMessagesUser() {
        val query: Query = messageProvider.getMessageByChat(idChat)
        val options = FirestoreRecyclerOptions.Builder<Message>()
            .setQuery(query, Message::class.java)
            .build()
        messageAdapter = MessageAdapter(options, context)
        binding.recyclerViewChat.adapter = messageAdapter
        messageAdapter?.startListening()
        messageAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                updateViewed()
                val numberMessage: Int = messageAdapter!!.itemCount
                val lastMessagePosition =
                    wrapContentLinearLayoutManager.findLastCompletelyVisibleItemPosition()

                if (lastMessagePosition == -1 || (positionStart >= (numberMessage - 1) && lastMessagePosition == (positionStart - 1))) {
                    binding.recyclerViewChat.scrollToPosition(positionStart)
                }
            }
        })
    }

    private fun updateViewed() {
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

    private fun sendMessage() {
        binding.buttonSend.setOnClickListener {
            saveMessage()
        }
    }

    private fun saveMessage() {
        val messageText = binding.editTextMessage.text.toString()
        if (messageText.isNotEmpty()) {
            message.setIdChat(idChat)
            if (authProvider.getUid() == idUserEmisor) {
                message.setIdEmisor(idUserEmisor)
                message.setIdReceptor(idUserReceptor)
            } else {
                message.setIdEmisor(idUserReceptor)
                message.setIdReceptor(idUserEmisor)
            }
            message.setTimeStamp(Date().time)
            message.setViewed(false)
            message.setIdChat(idChat)
            message.setMessage(messageText)

            messageProvider.create(message).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    binding.editTextMessage.setText("")
                    messageAdapter?.notifyDataSetChanged()
                    getToken(message)
                } else {
                    Toast.makeText(context, "Message Error, try again!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchRoomsFragment(view: View) {
        val goRooms = binding.buttonBackRooms

        navController = Navigation.findNavController(view)
        goRooms.setOnClickListener {
            navController.navigate(R.id.roomsFragment)
        }
    }

    private fun getUserInfo() {
        var idUserInfo = ""
        idUserInfo = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        chatRegistration = userProvider.getUserRealTime(idUserInfo)
            .addSnapshotListener { documentSnapshot, error ->
                if (documentSnapshot != null) {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("username")) {
                            usernameReceptor = documentSnapshot.getString("username").toString()
                            binding.textViewUsernameChat.text = usernameReceptor
                        }
                        if (documentSnapshot.contains("online")) {
                            val online: Boolean? = documentSnapshot.getBoolean("online")
                            if (online == true) {
                                binding.TextViewTimeChat.text = resources.getString(R.string.online)
                            } else if (documentSnapshot.contains("lastConnect")) {
                                val lastConnect: Long = documentSnapshot.getLong("lastConnect")!!.toLong()
                                val relativeTime = RelativeTime.getTimeAgo(lastConnect, context)
                                binding.TextViewTimeChat.text = relativeTime
                            }
                        }
                        if (documentSnapshot.contains("photo")) {
                            imageReceptor = documentSnapshot.getString("photo").toString()
                            if (imageReceptor.isNotEmpty()) {
                                Picasso.get().load(imageReceptor).resize(100, 100)
                                    .centerCrop().transform(CircleTransform())
                                    .into(binding.circleImageViewChat)
                            }
                        }
                    }
                }
            }
    }

    private fun getToken(message: Message) {
        var idUserForToken = ""
        idUserForToken = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        tokenProvider.getToken(idUserForToken).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot != null) {
                if(documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        val token: String = documentSnapshot.getString("token").toString()
                        getLastThreeMessages(message, token)
                    }
                } else {
                    Toast.makeText(context, "El token de notificaciones del usuario no existe", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getLastThreeMessages(message: Message, token: String) {
        messageProvider.getLastThreeMessageByChatEmisor(idChat, authProvider.getUid()).get().addOnSuccessListener { querySnapshot ->
            val messageArrayList = ArrayList<Message>()

            for (documentSnapshot in querySnapshot.documents) {
                if (documentSnapshot.exists()) {
                    val message1: Message? = documentSnapshot.toObject(Message::class.java)
                    messageArrayList.add(message1!!)
                }
            }
            if (messageArrayList.size == 0) {
                messageArrayList.add(message)
            }
            messageArrayList.reverse()

            val gson = Gson()
            val messages = gson.toJson(messageArrayList)
            sendNotification(token, messages, message)
        }
    }

    private fun sendNotification(token: String, messages: String, message: Message) {
        val data: MutableMap<String, String> = HashMap()
        data["title"] = "NEW MESSAGE"
        data["body"] = message.getMessage()
        data["idNotification"] = idNotificationChat.toString()
        data["messages"] = messages
        data["usernameEmisor"] = usernameEmisor.uppercase(Locale.getDefault())
        data["usernameReceptor"] = usernameReceptor.uppercase(Locale.getDefault())
        data["imageEmisor"] = imageEmisor
        data["imageReceptor"] = imageReceptor
        data["idEmisor"] = message.getIdEmisor()
        data["idReceptor"] = message.getIdReceptor()
        data["idChat"] = message.getIdChat()

        if (imageEmisor == "") { //TODO CAMBIAR A isEmpty y probar
            imageEmisor = "IMAGEN_NO_VALIDA"
        }
        if (imageReceptor == "") {
            imageReceptor = "IMAGEN_NO_VALIDA"
        }

        var idUserEmisorForNotification = ""
        idUserEmisorForNotification = if (authProvider.getUid() == idUserEmisor) {
            idUserReceptor
        } else {
            idUserEmisor
        }
        messageProvider.getLastMessageEmisor(idChat, idUserEmisorForNotification).get().addOnSuccessListener { querySnapshot ->
                val size: Int = querySnapshot.size()
                var lastMessage = ""
                if (size > 0) {
                    lastMessage = querySnapshot.documents[0].getString("message").toString()
                    data["lastMessage"] = lastMessage
                }
                val body = FCMBody(token, "high", "4500s", data)
                notificationProvider.sendNotification(body)?.enqueue(object : Callback<FCMResponse?> {
                        override fun onResponse(call: Call<FCMResponse?>?, response: Response<FCMResponse?>) {
                            if (response.body() != null) {
                                if (response.body()!!.getSuccess() == 1) {
                                    //Toast.makeText(context, "La notificacion se envio correcatemente", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "La notificacion no se pudo enviar", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<FCMResponse?>?, t: Throwable?) {}
                    })

        }
    }

    private fun getUsernameForNotifications() {
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

    private fun getConnectivity() {
            val connectivityManager = context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null && networkInfo.isConnected) {
                ViewedMessageHelper.updateState(true, requireContext())
            } else {
                binding.TextViewTimeChat.text = resources.getString(R.string.offline)
            }
    }
}

