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
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.DocumentSnapshot
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.databinding.ItemRoomsBinding
import com.squareup.picasso.Picasso
import com.jonathan.loginfuturo.model.Rooms
import com.jonathan.loginfuturo.providers.UserProvider
import java.util.*

class RoomsAdapter(options: FirestoreRecyclerOptions<Rooms>) : FirestoreRecyclerAdapter<Rooms, RoomsAdapter.RoomsHolder>(options) {

    private lateinit var navController: NavController
    private var bundle = Bundle()
    private lateinit var context: Context
    private lateinit var userProvider: UserProvider
    private lateinit var rooms: Rooms

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomsHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemRoomsBinding.inflate(layoutInflater, parent, false)

        userProvider = UserProvider()
        rooms = Rooms()

        return RoomsHolder(binding)
    }

   /* override fun getItemCount(): Int {
        return rooms.size
    }*/

    fun RoomsAdapter(options: FirestoreRecyclerOptions<Rooms>, context: Context) {
      //  super(options)
        this.context = context
    }
    //TODO MIRAR LA CARPETA 6 DEL VIDEO 7: MIN 9:50
    private fun getUserInfo(idUser: String, roomsHolder: RoomsHolder) {
        userProvider.getUser(idUser).addOnSuccessListener(object : OnSuccessListener<DocumentSnapshot> { //TODO CONVERTIR A LAMDA DESPUES Y VERIFICAR SI FUNCIONA ASI
            override fun onSuccess(documentSnapshot: DocumentSnapshot?) {
                if (documentSnapshot!!.exists()) {
                    if (documentSnapshot.contains("username")) {
                        val username : String = documentSnapshot.getString("username").toString()
                        roomsHolder.binding.textViewNameUser.text = username.uppercase(Locale.getDefault())
                    }
                    if (documentSnapshot.contains("image_profile")) {
                        val imageProfile : String = documentSnapshot.getString("image_profile").toString()
                        if (imageProfile.isEmpty()) {
                            Picasso.get()
                                .load(rooms.getPhoto())
                                .resize(100, 100)
                                .centerCrop()
                                .transform(CircleTransform())
                                .into(roomsHolder.binding.imageViewPhoto)
                        }
                    }
                }
            }
        })
    }

    override fun onBindViewHolder(roomsHolder: RoomsHolder, position: Int, rooms: Rooms) {
        val document = snapshots.getSnapshot(position)
        val roomsId = document.id
        val idUser : String = document.getString("idUser").toString()

        getUserInfo(idUser, roomsHolder)
       roomsHolder.render(rooms, roomsHolder)
        roomsHolder.bindingAdapterPosition


        roomsHolder.binding.root.setOnClickListener { view ->
            navController = Navigation.findNavController(view)
            bundle.putString("id", roomsId)
            navController.navigate(R.id.chatFragment)
        }
    }

    inner class RoomsHolder(val binding: ItemRoomsBinding) : RecyclerView.ViewHolder(binding.root) {

        fun render(rooms: Rooms, roomsHolder: RoomsHolder) = with(binding) {
         //   navController = Navigation.findNavController(binding.root)
        //    binding.root.setOnClickListener {
            binding.textViewNameUser.text = rooms.getEmail()
            binding.textViewMessageUser.text = rooms.getIdEmisor()
            binding.textViewStatusUser.text = rooms.getTimeStamp().toString()
            binding.textViewCountMessage.text = rooms.getIdReceptor()
                if (rooms.getPhoto().isEmpty()) {
                    Picasso.get()
                        .load(R.drawable.person).resize(100, 100)
                        .centerCrop()
                        .transform(CircleTransform())
                        .into(imageViewPhoto)
               //     navController.navigate(R.id.chatFragment)
                    binding.executePendingBindings()

                } else {
                    Picasso.get()
                        .load(rooms.getPhoto()).resize(100, 100)
                        .centerCrop()
                        .transform(CircleTransform())
                        .into(imageViewPhoto)
                    // binding.executePendingBindings()
               // }
            }
        }
    }
}

/*class RoomsAdapter(val rooms: List<Rooms>, val userId: String) : RecyclerView.Adapter<RoomsAdapter.RoomsHolder>() {

   // private lateinit var navController: NavController
    private lateinit var roomsColletion : CollectionReference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomsHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemRoomsBinding.inflate(layoutInflater, parent, false)

        roomsColletion = FirebaseFirestore.getInstance().collection("Users")

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
         //   navController = Navigation.findNavController(binding.root)
        //    binding.root.setOnClickListener {
            binding.textViewNameUser.text = rooms.getEmaill()
            binding.textViewMessageUser.text = rooms.getIdEmisor()
            binding.textViewStatusUser.text = rooms.getTimeStamp().toString()
            binding.textViewCountMessage.text = rooms.getIdReceptor()
                if (rooms.getPhoto().isEmpty()) {
                    Picasso.get()
                        .load(R.drawable.person).resize(100, 100)
                        .centerCrop()
                        .transform(CircleTransform())
                        .into(imageViewPhoto)
               //     navController.navigate(R.id.chatFragment)
                    binding.executePendingBindings()

                } else {
                    Picasso.get()
                        .load(rooms.getPhoto()).resize(100, 100)
                        .centerCrop()
                        .transform(CircleTransform())
                        .into(imageViewPhoto)
                    // binding.executePendingBindings()
               // }
            }
        }
    }
}*/

/*fun RoomsAdapter(options: FirestoreRecyclerOptions<Rooms>, context: Context) {
      //  super(options)
        this.context = context
    }
    //TODO MIRAR LA CARPETA 6 DEL VIDEO 7: MIN 9:50
    private fun getUserInfo(idUser: String, roomsHolder: RoomsHolder) {
        userProvider.getUser(idUser).addOnSuccessListener(object : OnSuccessListener<DocumentSnapshot> {
            override fun onSuccess(documentSnapshot: DocumentSnapshot?) {
                if (documentSnapshot!!.exists()) {
                    if (documentSnapshot.contains("username")) {
                        val username : String? = documentSnapshot.getString("username")
                        roomsHolder.binding.textViewNameUser.text = "username"
                    }
                    if (documentSnapshot.contains("image_profile")) {
                        val imageProfile : String? = documentSnapshot.getString("image_profile")
                        if (imageProfile != null) {
                            if (imageProfile.isEmpty()) {
                                Picasso.get()
                                    .load(rooms.getPhoto())
                                    .resize(100, 100)
                                    .centerCrop()
                                    .transform(CircleTransform())
                                    .into(roomsHolder.binding.imageViewPhoto)
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onBindViewHolder(roomsHolder: RoomsHolder, position: Int, rooms: Rooms) {
        val document = snapshots.getSnapshot(position)
        val postId = document.id //TODO CAMBIAR VARIABLES
        val idUser : String = document.getString("idUser").toString()

        getUserInfo(idUser, roomsHolder)
       roomsHolder.render(rooms, roomsHolder)
        roomsHolder.bindingAdapterPosition


        roomsHolder.binding.root.setOnClickListener { view ->
            navController = Navigation.findNavController(view)
            bundle.putString("id", postId)
            navController.navigate(R.id.chatFragment)
        }
    }*/

