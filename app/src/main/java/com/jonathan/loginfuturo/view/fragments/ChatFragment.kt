package com.jonathan.loginfuturo.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.model.Message
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.TotalMessagesEvent
import com.jonathan.loginfuturo.Utils.RxBus
import com.jonathan.loginfuturo.databinding.FragmentChatBinding
import com.jonathan.loginfuturo.view.adapters.ChatAdapter
import java.util.*
import java.util.EventListener
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatDataBaseReference : CollectionReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var chatAdapter : ChatAdapter


    private val fireBaseStore : FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private val messageList : ArrayList<Message> = ArrayList()

    private var chatSubscription : ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)

        setUpChatDataBase()
        setUPCurrentUser()
        setUpChatButton()
        setUpRecyclerView()
        //setUpAdapter()
        subscribeToChatMessage()

        return binding.root
    }

    /** CUANDO SE AGREGA ALGUN VALOR VERIFICA SI EXISTE, DE EXISTIR LA AÑADE Y SINO LA CREA**/

    private fun setUpChatDataBase() {
        chatDataBaseReference = fireBaseStore.collection("chat")
    }

    /** SI EL USUAROI ESTA LOGGEADO NO CIERRA SESION SOLO Y SINO LO MANDA A LA PANTALLA DE LOGGIN O CUALQUIER OTRA ACCION**/

    private fun setUPCurrentUser() {
            firebaseUser = firebaseAuth.currentUser!!
    }

    private fun setUpRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        chatAdapter = ChatAdapter(messageList, firebaseUser.uid)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = chatAdapter
    }

    private fun setUpChatButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString()
            if (messageText.isNotEmpty()) {
                val photo = firebaseUser.photoUrl?.let { firebaseUser.photoUrl.toString() } ?: run { "" }
                val message = Message(firebaseUser.uid, messageText, photo, Date())
                saveMessage(message)
                // Asi se borra el texto en el editText cuando se ha enviado el mensaje
                binding.editTextMessage.setText("")
            }
        }
    }

    private fun saveMessage(message: Message) {
        val newMessage = HashMap<String, Any>()
        newMessage["authorId"] = message.authorId
        newMessage["message"] = message.message
        newMessage["profileImageUrl"] = message.profileImageUrl
        newMessage["sendAt"] = message.sendAt

        chatDataBaseReference.add(newMessage)
            .addOnCompleteListener {
                Toast.makeText(context, "Message Added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Message Error, try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun subscribeToChatMessage() {
        chatSubscription = chatDataBaseReference
            .orderBy("sendAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener(object : EventListener, com.google.firebase.firestore.EventListener<QuerySnapshot> {
                override fun onEvent(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
                    exception?.let {
                        Toast.makeText(context, "Exception!", Toast.LENGTH_SHORT).show()
                        return
                    }
                    snapshot?.let {
                        messageList.clear()
                        val messages = it.toObjects(Message::class.java)
                        messageList.addAll(messages.asReversed())
                        chatAdapter.notifyDataSetChanged()
                        binding.recyclerView.smoothScrollToPosition(messageList.size)
                        RxBus.publish(TotalMessagesEvent(messageList.size)) //Envia un evento (TotalMessagesEvent) cuando se ejecute y todos lo que esten escuchando (subscribeTotalMessagesEventBusReactiveStyle()
                        // del InforFragment) a eventos del mismo tipo se ejecuta lo que esta en el codigo
                    }
                }
            })
    }


    override fun onDestroyView() {
        chatSubscription?.remove()
        super.onDestroyView()
    }
}

