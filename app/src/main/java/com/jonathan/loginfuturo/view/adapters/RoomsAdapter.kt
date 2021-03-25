package com.jonathan.loginfuturo.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Rooms
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.databinding.ItemRoomsBinding
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_rooms.view.*

class RoomsAdapter(val rooms : List<Rooms>, val userId : String) : RecyclerView.Adapter<RoomsAdapter.RoomsHolder>() {

    private lateinit var navController: NavController

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomsAdapter.RoomsHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemRoomsBinding.inflate(layoutInflater)

        return RoomsHolder(binding)
    }

    override fun getItemCount(): Int {
        return rooms.size
    }

    override fun onBindViewHolder(holder: RoomsHolder, position: Int) {
        holder.render(rooms[position])
    }

    inner class RoomsHolder(val binding: ItemRoomsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun render(rooms: Rooms) = with(binding) {
            navController = Navigation.findNavController(binding.root)
            binding.root.setOnClickListener {
            binding.textViewNameUser.text = rooms.userName
            binding.textViewMessageUser.text = rooms.messageUser
            binding.textViewStatusUser.text = rooms.statusUser
            binding.textViewCountMessage.text = rooms.countUser
                if (rooms.photo.isEmpty()) {
                    Picasso.get()
                        .load(R.drawable.person).resize(100, 100)
                        .centerCrop()
                        .transform(CircleTransform())
                        .into(imageViewPhoto)
                    navController.navigate(R.id.chatFragment)
                    binding.executePendingBindings()

                } else {
                    Picasso.get()
                        .load(rooms.photo).resize(100, 100)
                        .centerCrop()
                        .transform(CircleTransform())
                        .into(imageViewPhoto)
                    // binding.executePendingBindings()
                }
            }
        }
    }
}

