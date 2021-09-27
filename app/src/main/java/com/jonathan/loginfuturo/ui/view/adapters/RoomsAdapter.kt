package com.jonathan.loginfuturo.ui.view.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.ListenerRegistration
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.CircleTransform
import com.jonathan.loginfuturo.databinding.ItemRoomsBinding
import com.squareup.picasso.Picasso
import com.jonathan.loginfuturo.data.model.ChatModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.ChatProvider
import com.jonathan.loginfuturo.data.model.providers.MessageProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider

class RoomsAdapter(options: FirestoreRecyclerOptions<ChatModel>) : FirestoreRecyclerAdapter<ChatModel, RoomsAdapter.RoomsHolder>(options) {

    private lateinit var navController: NavController
    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var messageProvider: MessageProvider
    private lateinit var chatProvider: ChatProvider

    private var bundle = Bundle()
    private var context: Context? = null
    private var roomsSubscription: ListenerRegistration? = null
    private var roomsMessageSubscription: ListenerRegistration? = null

    constructor(options: FirestoreRecyclerOptions<ChatModel>, context: Context?) : this(options) {
        super.updateOptions(options)
        this.context = context
        userProvider = UserProvider()
        authProvider = AuthProvider()
        messageProvider = MessageProvider()
        chatProvider = ChatProvider()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomsHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemRoomsBinding.inflate(layoutInflater, parent, false)
        return RoomsHolder(binding)
    }

    override fun onBindViewHolder(roomsHolder: RoomsHolder, position: Int, chatModel: ChatModel) {
        val document = snapshots.getSnapshot(position)
        val roomsId = document.id

        if (authProvider.getUid() == chatModel.getIdEmisor()) {
            getUserInfo(chatModel.getIdReceptor(), roomsHolder)
        } else {
            getUserInfo(chatModel.getIdEmisor(), roomsHolder)
        }

        roomsHolder.render(chatModel)
        roomsHolder.bindingAdapterPosition

        roomsHolder.binding.root.setOnClickListener { view ->
            navController = Navigation.findNavController(view)
            bundle.putString("idChat", roomsId)
            bundle.putString("idEmisor", chatModel.getIdEmisor())
            bundle.putString("idReceptor", chatModel.getIdReceptor())
            navController.navigate(R.id.chatFragment, bundle)
        }

        getLastMessage(roomsId, roomsHolder)
        var idEmisor = ""
        idEmisor = if (authProvider.getUid() == chatModel.getIdEmisor()) {
            chatModel.getIdReceptor()
        } else {
            chatModel.getIdEmisor()
        }
        getMessageNotRead(roomsId, idEmisor, roomsHolder)
    }

    private fun getUserInfo(idUser: String, roomsHolder: RoomsHolder) {
        userProvider.getUser(idUser).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot!!.exists()) {
                if (documentSnapshot.contains("username")) {
                    val username: String = documentSnapshot.getString("username").toString()
                    roomsHolder.binding.textViewUsername.text = username
                }
                if (documentSnapshot.contains("photo")) {
                    val photo: String = documentSnapshot.getString("photo").toString()
                    if (photo.isNotEmpty()) {
                        Picasso.get().load(photo).resize(50, 50)
                            .centerCrop().transform(CircleTransform()).into(roomsHolder.binding.imageViewPhoto)
                    }
                }
            }
        }
    }

    private fun getLastMessage(roomsId: String, roomsHolder: RoomsHolder) {
        roomsMessageSubscription = messageProvider.getLastMessage(roomsId)
            .addSnapshotListener { querySnapshot, _ ->
                if (querySnapshot != null) {
                    val size: Int = querySnapshot.size()
                    if (size > 0) {
                        val lastMessage: String? = querySnapshot.documents[0].getString("message")
                        roomsHolder.binding.textViewLastMessage.text = lastMessage
                    }
                }
            }
    }

    fun getListener(): ListenerRegistration? {
        return roomsSubscription
    }

    fun getLastMessageListener(): ListenerRegistration? {
        return roomsMessageSubscription
    }

    private fun getMessageNotRead(roomsId: String, idEmisor: String, roomsHolder: RoomsHolder) {
        roomsSubscription = messageProvider.getMessageByChatEmisor(roomsId, idEmisor)
            .addSnapshotListener { querySnapshot, _ ->
                if (querySnapshot != null) {
                    val size: Int = querySnapshot.size()
                    if (size > 0) {
                        roomsHolder.binding.frameLayoutLastMessage.visibility = View.VISIBLE
                        roomsHolder.binding.textViewCountMessage.text = size.toString()
                    } else {
                        roomsHolder.binding.frameLayoutLastMessage.visibility = View.GONE
                    }
                }
            }
    }

    inner class RoomsHolder(val binding: ItemRoomsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun render(chatModel: ChatModel) {
            binding.textViewUsername.text = chatModel.getEmail()
        }
    }
}

