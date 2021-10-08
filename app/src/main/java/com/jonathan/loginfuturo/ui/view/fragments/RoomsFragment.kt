package com.jonathan.loginfuturo.ui.view.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.firestore.Query
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.databinding.FragmentRoomsBinding
import com.jonathan.loginfuturo.data.model.ChatModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.ChatProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import com.jonathan.loginfuturo.ui.view.activities.FindUserActivity
import com.jonathan.loginfuturo.ui.view.adapters.RoomsAdapter

class RoomsFragment : Fragment() {

    private lateinit var binding: FragmentRoomsBinding
    private lateinit var chatProvider: ChatProvider
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider

    private var roomsAdapter: RoomsAdapter? = null
    private var interstitial: InterstitialAd? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rooms, container, false)

        authProvider = AuthProvider()
        chatProvider = ChatProvider()
        userProvider = UserProvider()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkCompleteInfo()
        initInterstitialAd()
        initListeners()
    }

    override fun onStart() {
        super.onStart()
        getAllChatUser()
    }

    override fun onStop() {
        super.onStop()
        roomsAdapter?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (roomsAdapter?.getListener() != null) {
            roomsAdapter?.getListener()?.remove()
        }
        if (roomsAdapter?.getLastMessageListener() != null) {
            roomsAdapter?.getLastMessageListener()?.remove()
        }
    }

    private fun checkCompleteInfo() {
        val goCompleteFragment = binding.floatingButtonCompleteInfo

        goCompleteFragment.setOnClickListener {
            val intent = Intent(context, FindUserActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            showAds()
            initInterstitialAd()
        }
    }

    private fun getAllChatUser() {
        val layoutManager = LinearLayoutManager(context)
        val query: Query = chatProvider.getAll(authProvider.getUid())
        val options = FirestoreRecyclerOptions.Builder<ChatModel>()
            .setQuery(query, ChatModel::class.java)
            .build()
        roomsAdapter = RoomsAdapter(options, context)
        binding.recyclerViewRooms.layoutManager = layoutManager
        binding.recyclerViewRooms.adapter = roomsAdapter
        roomsAdapter?.startListening()
    }

    private fun initListeners() {
        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {}
            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}
            override fun onAdShowedFullScreenContent() { interstitial = null }
        }
    }

    //TODO CAMBIAR LOS IDS DE ADS(ANUNCIOS) EN EL XML Y EL MANIFEST CUANDO SE TERMINE LA APP, LOS ACTUALES SON DE PRUEBA (Ver strings o video de ari)
    private fun initInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(requireContext(), getString(R.string.test_interstitial), adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) { interstitial = interstitialAd }
            override fun onAdFailedToLoad(p0: LoadAdError) { interstitial = null } })
    }

    private fun showAds() {
        interstitial?.show(context as Activity)
    }
}