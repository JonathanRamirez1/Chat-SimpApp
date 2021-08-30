package com.jonathan.loginfuturo.view.fragments

import android.content.Intent
import android.content.Intent.getIntent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.auth.User
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.databinding.FragmentRoomsBinding
import com.jonathan.loginfuturo.model.Rooms
import com.jonathan.loginfuturo.model.UserModel
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.RoomsProvider
import com.jonathan.loginfuturo.providers.MessageProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.jonathan.loginfuturo.view.adapters.RoomsAdapter
import java.util.*
import kotlin.collections.ArrayList

class RoomsFragment : Fragment() {

    private lateinit var binding: FragmentRoomsBinding
    private lateinit var roomsAdapter: RoomsAdapter
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var roomsDataBaseReference: CollectionReference
    private lateinit var navController: NavController
    private lateinit var userModel: UserModel
    private lateinit var userProvider: UserProvider


    private val roomsList: ArrayList<Rooms> = ArrayList()
    private var mAdapter : RoomsAdapter? = null
    private var mRecyclerView: RecyclerView? = null
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private var bundle = Bundle()
    private lateinit var mExtraIdUser: String
    private lateinit var mRoomsProvider: RoomsProvider

    /* private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()
     private val mExtraidUser = Intent.getIntent("").getStringExtra("idUser")*/

    private var userEmisor = String()
    private var userReceptor = String()
    private var idChat = String()
    private var messageProvider = MessageProvider()
    private var authProvider = AuthProvider()
    private var roomsProvider : RoomsProvider = RoomsProvider()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rooms, container, false)



        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        launchFindUserFragment(view)
        //setUpRecyclerViewRooms()
        getIdUsers()
        checkChatExist()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userProvider = UserProvider()
        userModel = UserModel()
        setUPCurrentUser()


        mExtraIdUser = bundle.putString("idReceptor", String()).toString()
        mRoomsProvider = RoomsProvider()
    }

    private fun setUPCurrentUser() {
        firebaseUser = firebaseAuth.currentUser!!
    }

    override fun onStart() {
        super.onStart()
        setUpRecyclerViewRooms()
    }

    override fun onStop() {
        super.onStop()
        roomsAdapter.stopListening()
    }

    private fun setUpRecyclerViewRooms() { //TODO ALGUN ERROR CARPETA 6 VIDEO 7; 18:06
        val layoutManager = LinearLayoutManager(context)
        binding.recyclerViewRooms.layoutManager = layoutManager
        binding.recyclerViewRooms.itemAnimator = DefaultItemAnimator()
        val query: Query = mRoomsProvider.getComment(mExtraIdUser)
        val options: FirestoreRecyclerOptions<Rooms> =
            FirestoreRecyclerOptions.Builder<Rooms>()
                .setQuery(query, Rooms::class.java)
                .build()
         roomsAdapter = RoomsAdapter(options)
        binding.recyclerViewRooms.setHasFixedSize(true)
        binding.recyclerViewRooms.layoutManager = layoutManager
        binding.recyclerViewRooms.itemAnimator = DefaultItemAnimator()
        binding.recyclerViewRooms.adapter = roomsAdapter
        //mRecyclerView.adapter(mAdapter)
        roomsAdapter.startListening()

    }

   /* private fun setUpRecyclerViewRooms() {
        val layoutManager = LinearLayoutManager(context)
        roomsAdapter = RoomsAdapter(roomsList, firebaseUser.uid)

            binding.recyclerViewRooms.setHasFixedSize(true)
            binding.recyclerViewRooms.layoutManager = layoutManager
            binding.recyclerViewRooms.itemAnimator = DefaultItemAnimator()
            binding.recyclerViewRooms.adapter = roomsAdapter
        }*/

    private fun launchFindUserFragment(view: View) {
        val goFindUserFragment = binding.floatingButtonFindUser

        navController = Navigation.findNavController(view)
        goFindUserFragment.setOnClickListener {
            setDataUsers()
           // updatePhoto()
            navController.navigate(R.id.searchBarFragment)
        }
    }

   /* private fun updatePhoto() {
        val photo = firebaseUser.photoUrl.toString()
            val id: String = authProvider.getUid()
            userModel.setPhoto(photo)
            userModel.setId(id)
            userProvider.updateCollection(userModel)
    }*/

    private fun setDataUsers() {
        bundle.putString("idEmisor", authProvider.getUid())
        bundle.putString("idReceptor", mExtraIdUser)
        parentFragmentManager.setFragmentResult("key", bundle);

    }

    private fun checkChatExist() {
        roomsProvider.getOneToOneChat(userEmisor, userReceptor)?.get()?.addOnSuccessListener(object :
            OnSuccessListener<QuerySnapshot> {
            override fun onSuccess(queryDocumentSnapshot: QuerySnapshot) {
                val size: Int = queryDocumentSnapshot.size()
                if (size == 0) {
                    Toast.makeText(context, "El chat no existe", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "El chat existe", Toast.LENGTH_SHORT).show()
                    roomsAdapter.notifyDataSetChanged()
                }
            }
        })

    }

    /**ROOMS**/
    private fun getIdUsers() {
        userEmisor = arguments?.getString("idUser1").toString()
        userReceptor = arguments?.getString("idUser2").toString()
      //  idChat = arguments?.getString("idChat").toString()
        roomsProvider = RoomsProvider()
        messageProvider = MessageProvider()
        authProvider = AuthProvider()

        createChat()
    }

    /**ROOMS**/
    private fun createChat() {
        val rooms = Rooms()
        rooms.setIdEmisor(userEmisor)
        rooms.setIdReceptor(userReceptor)
        rooms.setWritting(false)
        rooms.setTimeStamp(Date())
        rooms.setId(userEmisor + userReceptor)

        val ids: ArrayList<String> = ArrayList()
        ids.add(userEmisor)
        ids.add(userReceptor)
        rooms.setIds(ids)
        roomsProvider.createCollectionUsers(rooms)
    }
}

/* //TODO ASI SE RESIVIRIA EL VALOR QUE VIENE DESDE EL OTRO FRAGMENT
private lateinit var holaquehace : String
        holaquehace = Intent().getStringExtra("hola").toString()


         //TODO USAR PARA ENVIAR DATOS A OTRO FRAGMENT Y SE DEBE LLAMAR EN EL CLICK QUE SE QUIERE RECIBIR
    private fun goSendHola(hola: String) {
        val intent = Intent(context, FindUserFragment::class.java)
        intent.putExtra("hola", hola)
        startActivity(intent)

        //TODO TENER EN CUENTA QUE EL METODO  goSendHola() se hace en un fragment y donde se recibe se hace en una activity (holaquehace)
    } */




