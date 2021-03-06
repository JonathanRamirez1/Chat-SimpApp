package com.jonathan.chatsimpapp.ui.view.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.databinding.FragmentFindUserBinding
import com.jonathan.chatsimpapp.data.model.UserModel
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider
import com.jonathan.chatsimpapp.data.model.providers.UserProvider
import com.jonathan.chatsimpapp.ui.view.activities.HomeActivity
import com.mancj.materialsearchbar.MaterialSearchBar
import com.jonathan.chatsimpapp.ui.view.adapters.FindUserAdapter
import java.util.*

class FindUserFragment : Fragment(), MaterialSearchBar.OnSearchActionListener {

    private lateinit var binding: FragmentFindUserBinding
    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var navController: NavController

    private var findUserAdapter: FindUserAdapter? = null
    private var mFindUserAdapter: FindUserAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, avedInstanceState: Bundle?): View {
        binding = FragmentFindUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchBar.setOnSearchActionListener(this)
        userProvider = UserProvider()
        authProvider = AuthProvider()
        authProvider.setUPCurrentUser()
        launchGoBackRooms()
        launchFindUserForGroupFragment(view)
    }

    private fun launchGoBackRooms() {
        val backRooms = binding.buttonBackRooms

        backRooms.setOnClickListener {
            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun launchFindUserForGroupFragment(view: View) {
        val groupChat = binding.linearLayoutFindUser

        navController = Navigation.findNavController(view)
        groupChat.setOnClickListener {
            navController.navigate(R.id.findUserForGroupFragment)
        }
    }

    override fun onSearchStateChanged(enabled: Boolean) {
        if (!enabled) {
            getAllEmail()
        }
        if (enabled) {
            binding.buttonBackRooms.visibility = View.GONE
        } else {
            binding.buttonBackRooms.visibility = View.VISIBLE
        }
    }

    override fun onSearchConfirmed(text: CharSequence?) {
        searchByEmail(text.toString().lowercase(Locale.getDefault()))
    }

    override fun onButtonClicked(buttonCode: Int) {}

    private fun searchByEmail(email: String) {
        val layoutManager = LinearLayoutManager(context)
        val query: Query = userProvider.getUserByEmail(email)
        val options = FirestoreRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .build()
        mFindUserAdapter = FindUserAdapter(options, context)
        binding.recyclerViewFindUser.layoutManager = layoutManager
        mFindUserAdapter?.notifyDataSetChanged()
        binding.recyclerViewFindUser.adapter = mFindUserAdapter
        mFindUserAdapter?.startListening()
    }

    private fun getAllEmail() {
        val layoutManager = LinearLayoutManager(context)
        val query: Query = userProvider.getAll()
        val options = FirestoreRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .build()
        findUserAdapter = FindUserAdapter(options, context)
        binding.recyclerViewFindUser.layoutManager = layoutManager
        findUserAdapter?.notifyDataSetChanged()
        binding.recyclerViewFindUser.adapter = findUserAdapter
        findUserAdapter?.startListening()
    }

    override fun onStart() {
        super.onStart()
        getAllEmail()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.hide()
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity?)?.supportActionBar?.show()
        findUserAdapter?.stopListening()
        if (mFindUserAdapter != null) {
            mFindUserAdapter?.stopListening()
        }
    }
}