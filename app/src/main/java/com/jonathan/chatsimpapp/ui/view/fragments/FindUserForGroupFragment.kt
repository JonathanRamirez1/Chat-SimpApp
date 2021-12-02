package com.jonathan.chatsimpapp.ui.view.fragments

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
import com.jonathan.chatsimpapp.data.model.UserModel
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider
import com.jonathan.chatsimpapp.data.model.providers.UserProvider
import com.jonathan.chatsimpapp.databinding.FragmentFindUserForGroupBinding
import com.jonathan.chatsimpapp.ui.view.adapters.FindUserForGroupAdapter
import com.mancj.materialsearchbar.MaterialSearchBar
import java.util.*

class FindUserForGroupFragment : Fragment(),  MaterialSearchBar.OnSearchActionListener {

    private lateinit var binding: FragmentFindUserForGroupBinding
    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var navController: NavController

    private var findUserForGroupAdapter: FindUserForGroupAdapter? = null
    private var mFindUserForGroupAdapter: FindUserForGroupAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFindUserForGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchBarForGroup.setOnSearchActionListener(this)
        userProvider = UserProvider()
        authProvider = AuthProvider()
        authProvider.setUPCurrentUser()
        launchFindUserFragment(view)
    }

    private fun launchFindUserFragment(view: View) {
        val goFindUser = binding.buttonBackFindUser

        navController = Navigation.findNavController(view)
        goFindUser.setOnClickListener {
            navController.navigate(R.id.findUserFragment)
        }
    }

    override fun onSearchStateChanged(enabled: Boolean) {
        if (!enabled) {
            getAllEmail()
        }
        if (enabled) {
            binding.buttonBackFindUser.visibility = View.GONE
        } else {
            binding.buttonBackFindUser.visibility = View.VISIBLE
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
        mFindUserForGroupAdapter = FindUserForGroupAdapter(options, context)
        binding.recyclerViewFindUserForGroup.layoutManager = layoutManager
        mFindUserForGroupAdapter?.notifyDataSetChanged()
        binding.recyclerViewFindUserForGroup.adapter = mFindUserForGroupAdapter
        mFindUserForGroupAdapter?.startListening()
    }

    private fun getAllEmail() {
        val layoutManager = LinearLayoutManager(context)
        val query: Query = userProvider.getAll()
        val options = FirestoreRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .build()
        findUserForGroupAdapter = FindUserForGroupAdapter(options, context)
        binding.recyclerViewFindUserForGroup.layoutManager = layoutManager
        findUserForGroupAdapter?.notifyDataSetChanged()
        binding.recyclerViewFindUserForGroup.adapter = findUserForGroupAdapter
        findUserForGroupAdapter?.startListening()
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
        findUserForGroupAdapter?.stopListening()
        if (mFindUserForGroupAdapter != null) {
            mFindUserForGroupAdapter?.stopListening()
        }
    }
}