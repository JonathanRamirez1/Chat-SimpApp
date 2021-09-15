package com.jonathan.loginfuturo.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.databinding.FragmentProfileBinding
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.OtherUserIdProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var navController: NavController
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider
    private lateinit var otherUserIdProvider: OtherUserIdProvider
    private var idEmisor: String = ""
    private var idReceptor: String = ""

    private var bundle = Bundle()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)

        authProvider = AuthProvider()
        otherUserIdProvider = OtherUserIdProvider()
        userProvider = UserProvider()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        launchChatFragment(view)
      //  getDataUsers()
        getUser()
    }

    private fun launchChatFragment(view: View) {
        val goChat = binding.floatingButtonGoChat

        navController = Navigation.findNavController(view)
        goChat.setOnClickListener {
          /* bundle = bundleOf("idEmisor" to idEmisor)
            bundle = bundleOf("idReceptor" to idReceptor)*/
            navController.navigate(R.id.chatFragment)
        }
    }

    private fun getDataUsers() {
            idEmisor = arguments?.getString("idEmisor").toString()
            idReceptor = arguments?.getString("idReceptor").toString()
    }

   /* private fun setDataUsers() {
            bundle.putString("idEmisor", authProvider.getUid())
            bundle.putString("idReceptor", mExtraIdUser)
            parentFragmentManager.setFragmentResult("requestKey", bundle)
    }*/

   /* private fun getIds() {
        otherUserIdProvider.getOtherUserId(mExtraIdUser).addOnSuccessListener { documentSnapshot ->
            if (documentSnapshot.exists()) {
                if (documentSnapshot.contains("otherId")) {
                    idUser = documentSnapshot.getString("otherId").toString() //TODO CARPETA 6 VIDEO 4
                    getUser(idUser)
                }
            }
        }
    }*/

    private fun getUser() {
        userProvider.getUser(idReceptor).addOnSuccessListener { documentSnapshot ->
            if(documentSnapshot.exists()) {
                if (documentSnapshot.contains("email")) {
                    val email: String = documentSnapshot.getString("email").toString()
                    binding.textViewEmailProfile.text = email
                }
                if (documentSnapshot.contains("username")) {
                    val username: String = documentSnapshot.getString("username").toString()
                    binding.textViewUsernameProfile.text = username
                }
                if (documentSnapshot.contains("photo")) {
                    val photo: String = documentSnapshot.getString("photo").toString()
                    Picasso.get().load(photo).resize(100, 100)
                        .centerCrop().transform(CircleTransform())
                        .into(binding.circleImageViewPhotoProfile)
                }
            }
        }
    }
}