package com.jonathan.chatsimpapp.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.databinding.FragmentProfileBinding
import com.jonathan.chatsimpapp.ui.viewmodels.ProfileViewModel

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    private val profileViewModel by viewModels<ProfileViewModel> {
        ProfileViewModel().createFactory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setObservers()
        profileViewModel.getUser()
        setTemplateNativeAdvanced()
    }

    private fun setObservers() {

        profileViewModel.emailProfile.observe(viewLifecycleOwner) { emailProfile ->
            binding.textViewEmailProfile.text = emailProfile
        }

        profileViewModel.usernameProfile.observe(viewLifecycleOwner) { usernameProfile ->
            binding.textViewUsernameProfile.text = usernameProfile
        }

        profileViewModel.phoneProfile.observe(viewLifecycleOwner) { phoneProfile ->
            binding.texViewPhone.text = phoneProfile
        }

        profileViewModel.dateProfile.observe(viewLifecycleOwner) { dateProfile ->
            binding.textViewDate.text = dateProfile
        }

        profileViewModel.coverProfile.observe(viewLifecycleOwner) { coverProfile ->
            coverProfile.into(binding.imageViewCover)
        }

        profileViewModel.photoProfile.observe(viewLifecycleOwner) { photoProfile ->
            photoProfile.into(binding.circleImageViewPhotoProfile)
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
        profileViewModel.getProfileListener()?.remove()
    }
}