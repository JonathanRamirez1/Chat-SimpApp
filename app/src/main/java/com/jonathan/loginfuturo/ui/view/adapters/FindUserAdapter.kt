package com.jonathan.loginfuturo.ui.view.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.CircleTransform
import com.jonathan.loginfuturo.databinding.ItemFindUserBinding
import com.jonathan.loginfuturo.data.model.UserModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import com.squareup.picasso.Picasso

class FindUserAdapter(options: FirestoreRecyclerOptions<UserModel>): FirestoreRecyclerAdapter<UserModel, FindUserAdapter.SearchBarHolder>(options) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindUserAdapter.SearchBarHolder {
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
            navController.navigate(R.id.chatFragment2, bundle)
        }
    }

    private fun getUserInfo(idUser: String, searchBarHolder: SearchBarHolder) {
        userProvider.getUser(idUser).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("username")) {
                    val username: String = documentSnapshot.getString("username").toString()
                    searchBarHolder.binding.textViewUsernameFind.text = username
                }
                if (documentSnapshot.contains("email")) {
                    val email: String = documentSnapshot.getString("email").toString()
                    searchBarHolder.binding.textViewEmailFind.text = email
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
            binding.textViewEmailFind.text = userModel.getEmail()
        }
    }
}