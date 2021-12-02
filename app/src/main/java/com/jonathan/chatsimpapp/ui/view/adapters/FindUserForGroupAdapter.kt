package com.jonathan.chatsimpapp.ui.view.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.jonathan.chatsimpapp.core.CircleTransform
import com.jonathan.chatsimpapp.data.model.UserModel
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider
import com.jonathan.chatsimpapp.data.model.providers.UserProvider
import com.jonathan.chatsimpapp.databinding.ItemFindUserForGroupBinding
import com.squareup.picasso.Picasso

class FindUserForGroupAdapter(options: FirestoreRecyclerOptions<UserModel>): FirestoreRecyclerAdapter<UserModel, FindUserForGroupAdapter.SearchBarHolder>(options) {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindUserForGroupAdapter.SearchBarHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemFindUserForGroupBinding.inflate(layoutInflater, parent, false)
        return SearchBarHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchBarHolder, position: Int, userModel: UserModel) {
        val document = snapshots.getSnapshot(position)
        val mExtraIdUser: String = document.getString("id").toString()

        if (authProvider.getUid() == mExtraIdUser) {
            holder.itemView.visibility = View.GONE
            holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
        }

        getUserInfo(mExtraIdUser, holder)
        holder.render(userModel)
        holder.bindingAdapterPosition
    }

    private fun checkBox() {

    }

    private fun getUserInfo(idUser: String, searchBarHolder: SearchBarHolder) {
        userProvider.getUser(idUser).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("username")) {
                    val username: String = documentSnapshot.getString("username").toString()
                    searchBarHolder.binding.textViewUsernameFindForGroup.text = username
                }
                if (documentSnapshot.contains("email")) {
                    val email: String = documentSnapshot.getString("email").toString()
                    searchBarHolder.binding.textViewEmailFindForGroup.text = email
                }
                if (documentSnapshot.contains("photo")) {
                    val photo: String = documentSnapshot.getString("photo").toString()
                    if (photo.isNotEmpty()) {
                        Picasso.get().load(photo).resize(100, 100)
                            .centerCrop().transform(CircleTransform())
                            .into(searchBarHolder.binding.imageViewPhotoForGroup)
                    }
                }
            }
        }
    }

    class SearchBarHolder(val binding: ItemFindUserForGroupBinding): RecyclerView.ViewHolder(binding.root) {

        fun render(userModel: UserModel) {
            binding.textViewEmailFindForGroup.text = userModel.getEmail()
        }
    }
}