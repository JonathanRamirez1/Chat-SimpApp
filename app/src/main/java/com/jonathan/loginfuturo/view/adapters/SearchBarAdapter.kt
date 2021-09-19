package com.jonathan.loginfuturo.view.adapters

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.databinding.ItemFindUserBinding
import com.jonathan.loginfuturo.model.UserModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.jonathan.loginfuturo.view.fragments.ProfileFragment
import com.squareup.picasso.Picasso


class SearchBarAdapter(options: FirestoreRecyclerOptions<UserModel>): FirestoreRecyclerAdapter<UserModel, SearchBarAdapter.SearchBarHolder>(options) {

    private lateinit var userProvider: UserProvider
    private lateinit var navController: NavController
    private lateinit var authProvider: AuthProvider

    private var context: Context? = null
    private var bundle = Bundle()

    constructor(options: FirestoreRecyclerOptions<UserModel>, context: Context?) : this(options) {
        super.updateOptions(options)
        this.context = context
        userProvider = UserProvider()
        authProvider = AuthProvider()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchBarAdapter.SearchBarHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemFindUserBinding.inflate(layoutInflater, parent, false)
        return SearchBarHolder(binding)
    }

    override fun onBindViewHolder(searchBarHolder: SearchBarHolder, position: Int, userModel: UserModel) {
        val document = snapshots.getSnapshot(position)
        val mExtraIdUser: String = document.getString("id").toString()

        if (authProvider.getUid() == mExtraIdUser) {
            searchBarHolder.itemView.visibility = View.GONE
            searchBarHolder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }

        getUserInfo(mExtraIdUser, searchBarHolder)
        searchBarHolder.render(userModel)
        searchBarHolder.bindingAdapterPosition

        searchBarHolder.binding.root.setOnClickListener { view ->
            navController = Navigation.findNavController(view)
            bundle.putString("idEmisor", authProvider.getUid())
            bundle.putString("idReceptor", mExtraIdUser)
            navController.navigate(R.id.chatFragment, bundle)
        }
    }



    private fun getUserInfo(idUser: String, searchBarHolder: SearchBarHolder) {
        userProvider.getUser(idUser).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("email")) {
                    val email: String = documentSnapshot.getString("email").toString()
                    searchBarHolder.binding.textViewEmailUser.text = email
                }
                if (documentSnapshot.contains("photo")) {
                    val photo: String = documentSnapshot.getString("photo").toString()
                    if (photo.isNotEmpty()) {
                        Picasso.get().load(photo).resize(100, 100)
                            .centerCrop().transform(CircleTransform())
                            .into(searchBarHolder.binding.imageViewPhoto)
                    }
                }
            }
        }
    }

    inner class SearchBarHolder(val binding: ItemFindUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun render(userModel: UserModel) {
            binding.textViewEmailUser.text = userModel.getEmail()
        }
    }
}