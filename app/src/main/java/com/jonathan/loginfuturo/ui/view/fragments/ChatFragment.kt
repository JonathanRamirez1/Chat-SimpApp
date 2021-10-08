package com.jonathan.loginfuturo.ui.view.fragments

import android.app.Activity
import android.content.Context.*
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.databinding.FragmentChatBinding
import com.jonathan.loginfuturo.data.model.Message
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.data.model.ChatModel
import com.jonathan.loginfuturo.ui.view.adapters.MessageAdapter
import java.util.*
import kotlin.collections.ArrayList
import com.google.firebase.firestore.DocumentSnapshot
import com.jonathan.loginfuturo.core.CircleTransform
import com.jonathan.loginfuturo.core.RelativeTime
import com.jonathan.loginfuturo.core.ViewedMessageHelper
import com.jonathan.loginfuturo.core.WrapContentLinearLayoutManager
import com.squareup.picasso.Picasso
import android.net.ConnectivityManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.jonathan.loginfuturo.data.model.FCMBody
import com.jonathan.loginfuturo.data.model.FCMResponse
import com.jonathan.loginfuturo.data.model.providers.*
import com.jonathan.loginfuturo.core.firebase.MessageReceiver
import com.jonathan.loginfuturo.ui.view.activities.HomeActivity
import retrofit2.Callback
import kotlin.collections.HashMap
import retrofit2.Call
import retrofit2.Response

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatProvider: ChatProvider
    private lateinit var chatModel: ChatModel
    private lateinit var message: Message
    private lateinit var messageProvider: MessageProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider
    private lateinit var tokenProvider: TokenProvider
    private lateinit var notificationProvider: NotificationProvider
    private lateinit var wrapContentLinearLayoutManager: WrapContentLinearLayoutManager

    private var messageAdapter: MessageAdapter? = null
    private var userInfoRegistration: ListenerRegistration? = null
    private var lastMessageEmisorRegistration: ListenerRegistration? = null
    private var messageReceiver: MessageReceiver? = null
    private var interstitial: InterstitialAd? = null

    private var idUserEmisor: String = ""
    private var idUserReceptor: String = ""
    private var idChat: String = ""
    private var usernameEmisor: String = ""
    private var usernameReceptor: String = ""
    private var imageEmisor: String = ""
    private var imageReceptor: String = ""
    private var idNotificationChat: Long? = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)

        chatProvider = ChatProvider()
        chatModel = ChatModel()
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

        getIds()
        getInfoUserForNotifications()
        sendMessage()
        launchRoomsFragment()
        getUserInfo()
        initInterstitialAd()
        initListeners()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.hide()
        val navBar: BottomNavigationView? = activity?.findViewById(R.id.bottomNavigation)
        navBar?.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        if (messageAdapter != null) {
            messageAdapter?.startListening()
        }
        ViewedMessageHelper.updateState(true, requireContext())
        getAllMessagesUser()
    }

    override fun onPause() {
        super.onPause()
        ViewedMessageHelper.updateState(false, requireContext())
    }

    override fun onStop() {
        super.onStop()
        messageAdapter?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        userInfoRegistration?.remove()
        lastMessageEmisorRegistration?.remove()
        messageReceiver?.getListenerLastMessageEmisor()?.remove()
    }

    /**Recuperación de los ids enviados desde roomsAdapter**/
    private fun getIds() {
        idUserEmisor = arguments?.getString("idEmisor").toString()
        idUserReceptor = arguments?.getString("idReceptor").toString()
        idChat = arguments?.getString("idChat").toString()
    }

    private fun checkChatExist() {
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

    private fun getAllMessagesUser() {
        chatModel.setId(idUserEmisor + idUserReceptor)
        idChat = chatModel.getId()
        val query: Query = messageProvider.getMessageByChat(idChat)
        val options = FirestoreRecyclerOptions.Builder<Message>()
            .setQuery(query, Message::class.java)
            .build()
        messageAdapter = MessageAdapter(options, context)
        binding.recyclerViewChat.adapter = messageAdapter
        wrapContentLinearLayoutManager = WrapContentLinearLayoutManager(context)
        wrapContentLinearLayoutManager.stackFromEnd = true
        binding.recyclerViewChat.layoutManager = wrapContentLinearLayoutManager
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
            checkChatExist()
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

    private fun launchRoomsFragment() {
        val goRooms = binding.buttonBackRooms

        goRooms.setOnClickListener {
            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            showAds()
            initInterstitialAd()
        }
    }

    private fun getUserInfo() {
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
                            binding.textViewUsernameChat.text = usernameReceptor
                        }
                        if (documentSnapshot.contains("online")) {
                            val online: Boolean? = documentSnapshot.getBoolean("online")
                            if (online == true) {
                                binding.TextViewTimeChat.text = resources.getString(R.string.online)

                            } else if (documentSnapshot.contains("lastConnect")) {
                                val lastConnect: Long =
                                    documentSnapshot.getLong("lastConnect")!!.toLong()
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
                if (documentSnapshot.exists()) {
                    if (documentSnapshot.contains("token")) {
                        val token: String = documentSnapshot.getString("token").toString()
                        getLastThreeMessages(message, token)
                    }
                } else {
                    Toast.makeText(
                        context,
                        "El token de notificaciones del usuario no existe",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun getLastThreeMessages(message: Message, token: String) {
        messageProvider.getLastThreeMessageByChatEmisor(idChat, authProvider.getUid()).get()
            .addOnSuccessListener { querySnapshot ->
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
        var lastMessage = ""
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
        lastMessageEmisorRegistration = messageProvider.getLastMessageEmisor(idChat, idUserEmisorForNotification)
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
                    notificationProvider.sendNotification(body)?.enqueue(object : Callback<FCMResponse?> {
                            override fun onResponse(call: Call<FCMResponse?>?, response: Response<FCMResponse?>) {
                                if (response.body() != null) {
                                    if (response.body()!!.getSuccess() == 1) {
                                       // Toast.makeText(context, "La notificacion se envio correctamente", Toast.LENGTH_SHORT).show()
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
    }

    private fun getInfoUserForNotifications() {
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

    private fun initListeners() {
        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {}
            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}
            override fun onAdShowedFullScreenContent() { interstitial = null }
        }
    }

    //TODO CAMBIAR LOS IDS DE ADS(ANUNCIOS) EN EL XML Y EL MANIFEST CUANDO SE TERMINE LA APP, LOS ACTUALES SON DE PRUEBA (Ver strings o video de ari)
    private fun initInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), getString(R.string.test_interstitial), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) { interstitial = interstitialAd }
            override fun onAdFailedToLoad(p0: LoadAdError) { interstitial = null } })
    }

    private fun showAds() {
        interstitial?.show(context as Activity)
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

