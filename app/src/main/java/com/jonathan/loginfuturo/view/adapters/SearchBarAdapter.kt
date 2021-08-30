package com.jonathan.loginfuturo.view.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_rates_item.view.*


class SearchBarAdapter(options: FirestoreRecyclerOptions<UserModel>): FirestoreRecyclerAdapter<UserModel, SearchBarAdapter.SearchBarHolder>(options) {

    private lateinit var userProvider: UserProvider
    private lateinit var navController: NavController
    private lateinit var authProvider: AuthProvider
    private lateinit var firebaseUser: FirebaseUser

    private var context: Context? = null
    private var bundle = Bundle()

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    constructor(options: FirestoreRecyclerOptions<UserModel>, context: Context?) : this(options) {
        super.updateOptions(options)
        this.context = context
        userProvider = UserProvider()
        authProvider = AuthProvider()
        setUPCurrentUser()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchBarAdapter.SearchBarHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemFindUserBinding.inflate(layoutInflater, parent, false)
        return SearchBarHolder(binding)
    }

    override fun onBindViewHolder(searchBarHolder: SearchBarHolder, position: Int, userModel: UserModel) {
        val document = snapshots.getSnapshot(position)
        val roomsId = document.id
        val id: String = document.getString("id").toString()

        getUserInfo(id, searchBarHolder)
        searchBarHolder.render(userModel)
        searchBarHolder.bindingAdapterPosition

        searchBarHolder.binding.root.setOnClickListener { view ->
            navController = Navigation.findNavController(view)
            bundle.putString("id", roomsId) //TODO VERIFICAR SI SIRVE PARA ALGO, TODAS
            navController.navigate(R.id.chatFragment)
        }
    }
    fun setUPCurrentUser() {
        firebaseUser = firebaseAuth.currentUser!!
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

    inner class SearchBarHolder(val binding: ItemFindUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun render(userModel: UserModel) {
            binding.textViewEmailUser.text = userModel.getEmail()
        }
    }
}

/*if (documentSnapshot.contains("photo")) {
                    val photo: String = documentSnapshot.getString("photo").toString()
                    if (photo.isNotEmpty()) {
                        Picasso.get().load(photo).resize(100, 100)
                            .centerCrop().transform(CircleTransform())
                            .into(searchBarHolder.binding.imageViewPhoto)
                    } else if (photo.isNullOrEmpty()) {
                        Picasso.get().load(R.drawable.person).resize(100, 100)
                            .centerCrop().transform(CircleTransform())
                            .into(searchBarHolder.binding.imageViewPhoto)
                    }
                }*/

/*if (photo.contains("2131165388")) {
                        Picasso.get().load(R.drawable.person).resize(100, 100)
                            .centerCrop().transform(CircleTransform())
                            .into(searchBarHolder.binding.imageViewPhoto)
                    } else {
                        Picasso.get().load(photo).resize(100, 100)
                            .centerCrop().transform(CircleTransform())
                            .into(searchBarHolder.binding.imageViewPhoto)
                    }*/

/*if (documentSnapshot.contains("photo")) {
                    val photoLocal: String = documentSnapshot.getString("photo").toString()
                    if (photoLocal.contains("2131165388") && photoLocal.isNullOrEmpty()) {
                        Picasso.get().load(photoLocal).resize(100, 100)
                            .centerCrop().transform(CircleTransform())
                            .into(searchBarHolder.binding.imageViewPhoto)
                    } else {
                        val photo: String = documentSnapshot.getString("photo").toString()
                        if (photo.isNotEmpty()) {
                            Picasso.get().load(photo).resize(100, 100)
                                .centerCrop().transform(CircleTransform())
                                .into(searchBarHolder.binding.imageViewPhoto)
                        }
                    }
                }*/