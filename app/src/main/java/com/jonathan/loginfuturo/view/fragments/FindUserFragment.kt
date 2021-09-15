package com.jonathan.loginfuturo.view.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.UserInformation
import com.jonathan.loginfuturo.databinding.FragmentFindUserBinding
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.MessageProvider
import com.jonathan.loginfuturo.view.adapters.FindUserAdapter
import kotlin.collections.ArrayList
import java.util.EventListener

class FindUserFragment : Fragment() {

    private lateinit var firebaseUser: FirebaseUser
    private lateinit var navController: NavController
    private lateinit var binding: FragmentFindUserBinding
    private lateinit var findUserAdapter: FindUserAdapter
    private lateinit var findUserDataBaseReference: CollectionReference

    private val listFindUser: ArrayList<UserInformation> = ArrayList()
    private val listFindUserFull: ArrayList<UserInformation> = ArrayList()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val mExtraidUser = Intent.getIntent("").getStringExtra("idUser")

    private var findUserSubscription: ListenerRegistration? = null

    private var userEmisor = String()
    private var userReceptor = String()
    private var idChat = String()
    private var messageProvider = MessageProvider()
    private var authProvider = AuthProvider()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_find_user, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchRoomsFragment(view)
        setUPCurrentUser()
        setUpRecyclerViewFindUser()
        setUpSearchViewFindUser()
    }

    /**Oculta el toolbar en este fragment al ponerlo en onResume y onStop**/
    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
    }

    private fun launchRoomsFragment(view: View) {
        val backRooms = binding.buttonBack

        navController = Navigation.findNavController(view)
        backRooms.setOnClickListener {
            navController.navigate(R.id.roomsFragment)
        }
    }

    private fun setUPCurrentUser() {
        firebaseUser = firebaseAuth.currentUser!!
    }

    private fun setUpRecyclerViewFindUser() {
        val layoutManager = LinearLayoutManager(context)
        findUserAdapter = FindUserAdapter(listFindUser, listFindUserFull)

        binding.recyclerViewFindUser.setHasFixedSize(true)
        binding.recyclerViewFindUser.layoutManager = layoutManager
        binding.recyclerViewFindUser.itemAnimator = DefaultItemAnimator()
        binding.recyclerViewFindUser.adapter = findUserAdapter
    }

    private fun setUpSearchViewFindUser() {
        binding.searchViewFindUser.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                findUserDataBaseReference = fireBaseStore.collection("Register")
                fireBaseStore.collection("Register")
                    .document(firebaseAuth.currentUser!!.email.toString()).get().addOnSuccessListener {
                        findUserSubscription = findUserDataBaseReference
                            .orderBy("email", Query.Direction.DESCENDING)
                            .addSnapshotListener(object : EventListener, com.google.firebase.firestore.EventListener<QuerySnapshot> {
                                override fun onEvent(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
                                    exception?.let {
                                        Toast.makeText(context, "Exception!", Toast.LENGTH_SHORT).show()
                                        return
                                    }
                                    snapshot?.let {
                                        val findUser = it.toObjects(UserInformation::class.java)
                                        listFindUserFull.clear()
                                        listFindUser.addAll(findUser)
                                        listFindUserFull.addAll(findUser)
                                        findUserAdapter.filter.filter(query)
                                        binding.recyclerViewFindUser.scrollToPosition(listFindUser.size)
                                        findUserAdapter.notifyDataSetChanged()
                                    }
                                }
                            })
                    }
                return true
            }
        })
    }

    override fun onDestroy() {
        findUserSubscription?.remove()
        super.onDestroy()
    }
}