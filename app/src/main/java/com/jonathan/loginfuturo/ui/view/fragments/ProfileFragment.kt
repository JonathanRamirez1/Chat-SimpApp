package com.jonathan.loginfuturo.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.firebase.firestore.ListenerRegistration
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.CircleTransform
import com.jonathan.loginfuturo.databinding.FragmentProfileBinding
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.MessageProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_rates.view.*
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider
    private lateinit var messageProvider: MessageProvider

    private var profileRegistration: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_profile, container, false)

        authProvider = AuthProvider()
        userProvider = UserProvider()
        messageProvider = MessageProvider()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
                            binding.textViewGender.text = SimpleDateFormat("dd MMMM, yyyy").format(timeStamp)
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
