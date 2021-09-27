package com.jonathan.loginfuturo.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.databinding.FragmentGroupChatBinding
import com.jonathan.loginfuturo.data.model.Message
import com.jonathan.loginfuturo.ui.view.adapters.GroupChatAdapter


class GroupChatFragment : Fragment() {

    private lateinit var binding: FragmentGroupChatBinding
    private lateinit var navController: NavController
    private lateinit var groupChatAdapter: GroupChatAdapter
    private lateinit var chatDataBaseReference: CollectionReference
    private lateinit var firebaseUser: FirebaseUser

    private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val messageList: ArrayList<Message> = ArrayList()
    private var chatSubscription: ListenerRegistration? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_group_chat, container, false)
        return binding.root
    }

    private fun setUpRecyclerView() {
      val layoutManager = LinearLayoutManager(context)
      groupChatAdapter = GroupChatAdapter(messageList, firebaseUser.uid)

      binding.recyclerView.setHasFixedSize(true)
      binding.recyclerView.layoutManager = layoutManager
      binding.recyclerView.itemAnimator = DefaultItemAnimator()
      binding.recyclerView.adapter = groupChatAdapter
  }

  private fun setUpChatButton() {
        binding.buttonSend.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString()
            if (messageText.isNotEmpty()) {
                val photo = firebaseUser.photoUrl?.let { firebaseUser.photoUrl.toString() } ?: run { "" }
                val email = firebaseUser.email?.let { firebaseUser.email } ?: run { "" }
               /* val authorId = firebaseAuth.currentUser?.uid.let { firebaseAuth.currentUser!!.uid }
                val receptorId = firebaseAuth.currentUser?.uid.let { firebaseAuth.currentUser!!.uid }*/
                //fireBaseStore.collection("chat").document(authorId).get().addOnSuccessListener {
                   // val message = Message(authorId, messageText, photo, Date(), email, receptorId)
                  //  saveMessage(message)
                    // Asi se borra el texto en el editText cuando se ha enviado el mensaje
                    binding.editTextMessage.setText("")
               // }
            }
        }
    }

    private fun setUpChatDataBase() {
        chatDataBaseReference = fireBaseStore.collection("chat")
    }

        /*  private fun saveMessage(message: Message) {
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
       }*/

      override fun onDestroyView() {
           chatSubscription?.remove()
           super.onDestroyView()
       }
}