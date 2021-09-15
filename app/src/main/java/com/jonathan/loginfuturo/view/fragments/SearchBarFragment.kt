package com.jonathan.loginfuturo.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Utils.ViewedMessageHelper
import com.jonathan.loginfuturo.databinding.FragmentSearchBarBinding
import com.jonathan.loginfuturo.model.UserModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.mancj.materialsearchbar.MaterialSearchBar
import com.jonathan.loginfuturo.view.adapters.SearchBarAdapter
import java.util.*


class SearchBarFragment : Fragment(), MaterialSearchBar.OnSearchActionListener {

    private lateinit var binding: FragmentSearchBarBinding
    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var searchBarAdapter: SearchBarAdapter

    private var mSearchBarAdapter: SearchBarAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, avedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search_bar, container, false)
        binding.searchBar.setOnSearchActionListener(this)

        userProvider = UserProvider()
        authProvider = AuthProvider()
        authProvider.setUPCurrentUser()

        val layoutManager = LinearLayoutManager(context)  //TODO PONER EN EL searchByEmail() Y getAllEmail() Y DESPUES PROBAR SI FUNCIONA
        binding.recyclerViewFindUser.layoutManager = layoutManager

        return binding.root
    }

    override fun onSearchStateChanged(enabled: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onSearchConfirmed(text: CharSequence?) {
        searchByEmail(text.toString().lowercase(Locale.getDefault()))
    }

    override fun onButtonClicked(buttonCode: Int) {
        TODO("Not yet implemented")
    }

    private fun searchByEmail(email: String) {
        val query: Query = userProvider.getUserByEmail(email)
        val options = FirestoreRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .build()
        mSearchBarAdapter = SearchBarAdapter(options, context)
        mSearchBarAdapter?.notifyDataSetChanged()
        binding.recyclerViewFindUser.adapter = mSearchBarAdapter
        mSearchBarAdapter?.startListening()

    }

    private fun getAllEmail() {
        val query: Query = userProvider.getAll()
        val options = FirestoreRecyclerOptions.Builder<UserModel>()
            .setQuery(query, UserModel::class.java)
            .build()
        searchBarAdapter = SearchBarAdapter(options, context)
        binding.recyclerViewFindUser.adapter = searchBarAdapter
        searchBarAdapter.startListening()
    }

    override fun onStart() {
        super.onStart()
        (activity as AppCompatActivity?)!!.supportActionBar!!.hide()
        getAllEmail()
    }

    override fun onStop() {
        super.onStop()
        searchBarAdapter.stopListening()
        mSearchBarAdapter?.stopListening()
    }
}