package com.jonathan.loginfuturo.view.adapters

import android.app.Application
import android.content.Intent
import android.content.Intent.getIntent
import android.content.Intent.getIntentOld
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.UserInformation
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.databinding.ItemFindUserBinding
import com.jonathan.loginfuturo.providers.AuthProvider
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_find_user.view.*
import java.util.*
import kotlin.collections.ArrayList


class FindUserAdapter(private var findUser: List<UserInformation>, private var findUserFull: List<UserInformation>): RecyclerView.Adapter<FindUserAdapter.FindUserHolder>(), Filterable {

    private lateinit var binding: ItemFindUserBinding
    private lateinit var navController: NavController

    private val firebaseAuth = AuthProvider()
    private val mExtraidUser = getIntent("").getStringExtra("idUser")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FindUserHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = ItemFindUserBinding.inflate(layoutInflater, parent, false)
           launchChatFragment(parent)

        return FindUserHolder(binding.root)
    }

    override fun onBindViewHolder(holder: FindUserHolder, position: Int) {
       return holder.bind(findUser[position])
    }

    override fun getItemCount(): Int {
        return findUser.size
    }

    inner class FindUserHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(userInformation: UserInformation) = with(itemView) {
            textViewEmailUser.text = userInformation.email
            if (userInformation.photo.isEmpty()) {
                Picasso.get()
                    .load(R.drawable.person).resize(100, 100)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(imageViewPhoto)
            } else {
                Picasso.get()
                    .load(userInformation.photo).resize(100, 100)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(imageViewPhoto)
            }
        }
    }

    private fun launchChatFragment(itemView: View) {
        val goToChat = binding.root
        val bundle = Bundle()
        navController = Navigation.findNavController(itemView)
        goToChat.setOnClickListener {
          /*  bundle.putString("idEmisor", firebaseAuth.getUid())
            bundle.putString("idReceptor", mExtraidUser)*/
            navController.navigate(R.id.chatFragment)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredList: MutableList<UserInformation> = ArrayList()
                if (constraint == null || constraint.isEmpty()) {
                    filteredList.addAll(findUserFull)
                } else {
                    val filterPattern = constraint.toString().toLowerCase(Locale.ROOT).trim()
                    for (item in findUser) {
                        if (item.toString().toLowerCase(Locale.ROOT).contains(filterPattern)) {
                            filteredList.add(item)
                        }
                    }
                }
                val filterResult = FilterResults()
                filterResult.values = filteredList
                return filterResult
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults) {
                  //  (findUser as ArrayList<UserInformation>).clear()
                   // (findUser as ArrayList<UserInformation>).addAll(results.values as ArrayList<UserInformation>)
                findUser = results.values as ArrayList<UserInformation>
                    notifyDataSetChanged()
            }
        }
    }
}