package com.jonathan.loginfuturo.view.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.databinding.FragmentRoomsBinding
import com.jonathan.loginfuturo.model.ChatModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.ChatProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.jonathan.loginfuturo.view.activities.CompleteInfoActivity
import com.jonathan.loginfuturo.view.activities.FindUserActivity
import com.jonathan.loginfuturo.view.adapters.RoomsAdapter

class RoomsFragment : Fragment() {

    private lateinit var binding: FragmentRoomsBinding
    private lateinit var chatProvider: ChatProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider

    private  var roomsAdapter: RoomsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rooms, container, false)

        authProvider = AuthProvider()
        chatProvider = ChatProvider()
        userProvider = UserProvider()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkCompleteInfo()
    }

    override fun onStart() {
        super.onStart()
        getAllChatUser()
    }

    override fun onStop() {
        super.onStop()
        roomsAdapter?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (roomsAdapter?.getListener() != null) {
            roomsAdapter?.getListener()?.remove()
        }
        if (roomsAdapter?.getLastMessageListener() != null) {
            roomsAdapter?.getLastMessageListener()?.remove()
        }
    }

    private fun checkCompleteInfo() {
        val goCompleteFragment = binding.floatingButtonCompleteInfo
        val id: String = authProvider.getUid()

        goCompleteFragment.setOnClickListener {

            userProvider.getUser(id).addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val cover: String = documentSnapshot.getString("cover").toString()
                    val phone: String = documentSnapshot.getString("phone").toString()
                    val photo: String = documentSnapshot.getString("photo").toString()
                    val username: String = documentSnapshot.getString("username").toString()
                    val gender: String = documentSnapshot.getString("gender").toString()
                    if (cover.isEmpty() && phone.isEmpty() && photo.isEmpty() && username.isEmpty() && gender.isEmpty()) {
                        val intent = Intent(context, CompleteInfoActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        val intent = Intent(context, FindUserActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    }
                }
            }
        }
    }

    private fun getAllChatUser() {
        val layoutManager = LinearLayoutManager(context)
        val query: Query = chatProvider.getAll(authProvider.getUid())
        val options = FirestoreRecyclerOptions.Builder<ChatModel>()
            .setQuery(query, ChatModel::class.java)
            .build()
        roomsAdapter = RoomsAdapter(options, context)
        binding.recyclerViewRooms.layoutManager = layoutManager
        binding.recyclerViewRooms.adapter = roomsAdapter
        roomsAdapter?.startListening()
    }
}





