package com.jonathan.loginfuturo.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Rooms
import com.jonathan.loginfuturo.databinding.FragmentRoomsBinding
import com.jonathan.loginfuturo.view.adapters.RoomsAdapter
import kotlinx.android.synthetic.main.fragment_rooms.*

class RoomsFragment : Fragment() {

    private lateinit var binding: FragmentRoomsBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var roomsDataBaseReference : CollectionReference


    private val roomsList : ArrayList<Rooms> = ArrayList()
    private val firebaseAuth : FirebaseAuth = FirebaseAuth.getInstance()
    private val fireBaseStore : FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rooms, container, false)

        setUPCurrentUser()
        setUpRecyclerViewRooms()
        setUpChatDataBase()

        return binding.root
    }

    private fun setUpChatDataBase() {
        roomsDataBaseReference = fireBaseStore.collection("rooms")
    }

    private fun setUPCurrentUser() {
        firebaseUser = firebaseAuth.currentUser!!
    }

    private fun setUpRecyclerViewRooms() {
        val layoutManager = LinearLayoutManager(context)
        roomsAdapter = RoomsAdapter(roomsList, firebaseUser.uid)

      //  if (recyclerViewRooms != null) {
            binding.recyclerViewRooms.setHasFixedSize(true)
            binding.recyclerViewRooms.layoutManager = layoutManager
            binding.recyclerViewRooms.itemAnimator = DefaultItemAnimator()
            binding.recyclerViewRooms.adapter = roomsAdapter

       // }
    }

}