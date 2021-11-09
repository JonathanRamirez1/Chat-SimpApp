package com.jonathan.chatsimpapp.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.ListenerRegistration
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.CircleTransform
import com.jonathan.chatsimpapp.databinding.FragmentProfileBinding
import com.jonathan.chatsimpapp.data.model.providers.AuthProvider
import com.jonathan.chatsimpapp.data.model.providers.UserProvider
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider

    private var profileRegistration: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authProvider = AuthProvider()
        userProvider = UserProvider()
        getUser()
        setTemplateNativeAdvanced()
    }

    private fun getUser() {
        profileRegistration = userProvider.getUserRealTime(authProvider.getUid())
            .addSnapshotListener { documentSnapshot, _ ->
                if (documentSnapshot != null) {
                    if (documentSnapshot.exists()) {
                        if (documentSnapshot.contains("email")) {
                            val email: String = documentSnapshot.getString("email").toString()
                            binding.textViewEmailProfile.text = email
                        }
                        if (documentSnapshot.contains("username")) {
                            val username: String = documentSnapshot.getString("username").toString()
                            binding.textViewUsernameProfile.text = username
                        }
                        if (documentSnapshot.contains("phone")) {
                            val phone: String = documentSnapshot.getString("phone").toString()
                            binding.texViewPhone.text = phone
                        }
                        if (documentSnapshot.contains("timeStamp")) {
                            val timeStamp: Date? = documentSnapshot.getDate("timeStamp")
                            binding.textViewDate.text = SimpleDateFormat("dd MMMM, yyyy").format(timeStamp)
                        }
                        if (documentSnapshot.contains("cover")) {
                            val cover: String = documentSnapshot.getString("cover").toString()
                            if (cover.isEmpty()) {
                                Picasso.get().load(R.drawable.person).into(binding.imageViewCover)
                            } else {
                                Picasso.get().load(cover).into(binding.imageViewCover)
                            }
                        }
                        if (documentSnapshot.contains("photo")) {
                            val photo: String = documentSnapshot.getString("photo").toString()
                            if (photo.isEmpty()) {
                                Picasso.get().load(R.drawable.person)
                                    .into(binding.circleImageViewPhotoProfile)
                            } else {
                                Picasso.get().load(photo).resize(100, 100)
                                    .centerCrop().transform(CircleTransform())
                                    .into(binding.circleImageViewPhotoProfile)
                            }
                        }
                    }
                }
            }
    }

    private fun setTemplateNativeAdvanced() {
        val adRequest = AdRequest.Builder().build()
        MobileAds.initialize(requireContext())
        val adLoader: AdLoader = AdLoader.Builder(requireContext(), getString(R.string.test_native_advanced))
            .forNativeAd { nativeAd ->
                val template = binding.templateProfile
                template.setNativeAd(nativeAd)
            }
            .build()

        adLoader.loadAd(adRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (profileRegistration != null) {
            profileRegistration?.remove()
        }
    }
}