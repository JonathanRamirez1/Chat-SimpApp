package com.jonathan.loginfuturo.view.fragments

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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.TotalMessagesEvent
import com.jonathan.loginfuturo.Utils.RxBus
import com.jonathan.loginfuturo.databinding.FragmentChatBinding
import com.jonathan.loginfuturo.model.Message
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.RoomsProvider
import com.jonathan.loginfuturo.providers.MessageProvider
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.model.Rooms
import com.jonathan.loginfuturo.view.adapters.ChatAdapter
import java.util.*
import kotlin.collections.ArrayList


class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatDataBaseReference: CollectionReference
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var navController: NavController

    private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val messageList: ArrayList<Message> = ArrayList()

    private var userEmisor: String? = null
    private var userReceptor: String? = null
    private var roomsProvider: RoomsProvider? = null
    private var idChat = String()

   // private val roomsProvider: RoomsProvider = RoomsProvider()
    //TODO PONER EN NULO TAL VEZ
    private var messageProvider = MessageProvider()
    private var authProvider = AuthProvider()
    private var chatSubscription: ListenerRegistration? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parentFragmentManager.setFragmentResultListener("key", this, { _, bundle ->
            userEmisor = bundle.getString("idEmisor")
            userReceptor = bundle.getString("idReceptor")
            roomsProvider = RoomsProvider()

            createChat()
        })
    }

    private fun createChat() {
        val rooms = Rooms()
        userEmisor?.let { rooms.setIdEmisor(it) }
        userReceptor?.let { rooms.setIdReceptor(it) }
        rooms.setWritting(false)
        rooms.setTimeStamp(Date())
        rooms.setId(userEmisor + userReceptor)

        val ids: ArrayList<String> = ArrayList()
        userEmisor?.let { ids.add(it) }
        userReceptor?.let { ids.add(it) }
        rooms.setIds(ids)
        roomsProvider?.createCollectionUsers(rooms)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  getIdUsers()
        //  setUpChatDataBase()
        setUPCurrentUser()
        //   setUpChatButton()
        setUpRecyclerView()
        //  subscribeToChatMessage()
        launchRoomsFragment(view)
        saveMessage()
        // checkChatExist()

    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)!!.supportActionBar!!.show()
    }


    /** CUANDO SE AGREGA ALGUN VALOR VERIFICA SI EXISTE, DE EXISTIR LA AÑADE Y SINO LA CREA**/

    /* private fun setUpChatDataBase() {
        chatDataBaseReference = fireBaseStore.collection("chat")
    }*/

    /** SI EL USUAROI ESTA LOGGEADO NO CIERRA SESION SOLO Y SINO LO MANDA A LA PANTALLA DE LOGGIN O CUALQUIER OTRA ACCION**/

    private fun setUPCurrentUser() {
        firebaseUser = firebaseAuth.currentUser!!
    }

    private fun launchRoomsFragment(view: View) {
        val goRooms = binding.buttonBackRooms

        navController = Navigation.findNavController(view)
        goRooms.setOnClickListener {
            navController.navigate(R.id.roomsFragment)
        }

    }

    private fun setUpRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        chatAdapter = ChatAdapter(messageList, firebaseUser.uid)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = chatAdapter
    }

    /* private fun setUpChatButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString()
            if (messageText.isNotEmpty()) {
                val photo = firebaseUser.photoUrl?.let { firebaseUser.photoUrl.toString() } ?: run { "" }
                val email = firebaseUser.email?.let { firebaseUser.email } ?: run { "" }
               /* val authorId = firebaseAuth.currentUser?.uid.let { firebaseAuth.currentUser!!.uid }
                val receptorId = firebaseAuth.currentUser?.uid.let { firebaseAuth.currentUser!!.uid }*/
                //fireBaseStore.collection("chat").document(authorId).get().addOnSuccessListener {
                    val message = Message(authorId, messageText, photo, Date(), email, receptorId)
                    saveMessage(message)
                    // Asi se borra el texto en el editText cuando se ha enviado el mensaje
                    binding.editTextMessage.setText("")
               // }
            }
        }
    }*/

    /**Chat**/
    private fun saveMessage() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString()
            val photo =
                firebaseUser.photoUrl?.let { firebaseUser.photoUrl?.toString() } ?: run { "" }
            if (messageText.isNotEmpty()) {
                val message = Message()
                message.setIdChat(idChat)
                if (authProvider.getUid().equals(userEmisor)) {
                    userEmisor?.let { it1 -> message.setIdEmisor(it1) }
                    userReceptor?.let { it1 -> message.setIdReceptor(it1) }
                } else {
                    userReceptor?.let { it1 -> message.setIdEmisor(it1) }
                    userEmisor?.let { it1 -> message.setIdReceptor(it1) }
                }
                message.setTimeStamp(Date().time)
                message.setViewed(false)
                message.setIdChat(idChat)
                message.setMessage(messageText)
                message.setProfileImageUrl(photo)

                messageProvider.create(message)
                    .addOnCompleteListener(object : OnCompleteListener<Void> {
                        override fun onComplete(task: Task<Void>) {
                            if (task.isSuccessful) {
                                // messageList.clear()
                                binding.editTextMessage.setText("")
                                messageList.addAll(listOf(message))
                                chatAdapter.notifyDataSetChanged()
                                binding.recyclerView.smoothScrollToPosition(messageList.size)
                                RxBus.publish(TotalMessagesEvent(messageList.size))
                                Toast.makeText(context, "Message Added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Message Error, try again!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    })
            }
        }
    }
}
   /* private fun saveMessage(message: Message) {
        val newMessage = HashMap<String, Any>()
        newMessage["id"] = message.id
        newMessage["authorId"] = message.idEmisor
        newMessage["receptorId"] = message.idReceptor
        newMessage["idChat"] = message.idChat
        newMessage["message"] = message.message
        newMessage["timeStamp"] = message.timeStamp
        newMessage["profileImageUrl"] = message.profileImageUrl

        chatDataBaseReference.add(newMessage)
            .addOnCompleteListener {
                Toast.makeText(context, "Message Added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Message Error, try again!", Toast.LENGTH_SHORT).show()
            }
    }*/
      /*  private fun subscribeToChatMessage() {
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
    }*/

 /*   override fun onDestroyView() {
        chatSubscription?.remove()
        super.onDestroyView()
    }*/

