package com.jonathan.loginfuturo.ui.view.fragments

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.objects.RxBus
import com.jonathan.loginfuturo.data.model.Rate
import com.jonathan.loginfuturo.core.RateDialog
import com.jonathan.loginfuturo.databinding.FragmentRatesBinding
import com.jonathan.loginfuturo.data.model.NewRateEvent
import com.jonathan.loginfuturo.ui.view.adapters.RatesAdapter
import io.reactivex.disposables.Disposable
import java.util.EventListener
import kotlin.collections.ArrayList

class RatesFragment : Fragment() {

    private lateinit var binding: FragmentRatesBinding
    private lateinit var ratesAdapter: RatesAdapter
    private lateinit var ratesDataBaseReference: CollectionReference
    private lateinit var rateBusListener: Disposable
    private lateinit var firebaseUser: FirebaseUser

    private var ratesSubscription: ListenerRegistration? = null
    private var interstitial: InterstitialAd? = null

    private val ratesList: ArrayList<Rate> = ArrayList()
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRatesBinding.inflate(inflater, container, false)
        return  binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUpRateDataBase()
        setUPCurrentUser()
        setUpRecyclerView()
        sepUpFab()
        subscribeToRatings()
        subscribeToNewRatings()
        initInterstitialAd()
        initListeners()
    }

    /** CUANDO SE AGREGA ALGUN VALOR VERIFICA SI EXISTE, DE EXISTIR LA AÑADE Y SINO LA CREA**/

    private fun setUpRateDataBase() {
        ratesDataBaseReference = fireBaseStore.collection("rates")
    }

    /** SI EL USUARIO ESTA LOGEADO LO DA Y SINO LO MANDA A LA PANTALLA DE LOGIN O CUALQUIER OTRA ACCION**/

    private fun setUPCurrentUser() {
            firebaseUser = firebaseAuth.currentUser!!
    }

    private fun setUpRecyclerView() {
        val layoutManager = LinearLayoutManager(context)
        ratesAdapter = RatesAdapter(ratesList)

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
        binding.recyclerView.adapter = ratesAdapter
    }

    private fun sepUpFab() {
        binding.fabRating.setOnClickListener {
            fragmentManager?.let { it1 -> RateDialog().show(it1, "") }
            showAds()
            initInterstitialAd()
        }
    }

    private fun saveRate(rate: Rate) {
        val newRating = HashMap<String, Any>()
        newRating["text"] = rate.text
        newRating["rate"] = rate.rate
        newRating["createAt"] = rate.createAt
        newRating["profileImageUrl"] = rate.profileImageUrl

        ratesDataBaseReference.add(newRating)
            .addOnCompleteListener {
                Toast.makeText(context, "Rating added!", Toast.LENGTH_SHORT).show()
            }

            .addOnFailureListener {
                Toast.makeText(context, "Rating error, try again!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun subscribeToRatings() {
        ratesSubscription = ratesDataBaseReference
            .orderBy("createAt", Query.Direction.DESCENDING)
            .addSnapshotListener(object : EventListener, com.google.firebase.firestore.EventListener<QuerySnapshot> {
                override fun onEvent(snapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {

                    exception?.let {
                        Toast.makeText(context, "Exception!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    snapshot?.let {
                        ratesList.clear()
                        val rates = it.toObjects(Rate::class.java)
                        ratesList.addAll(rates)
                        ratesAdapter.notifyDataSetChanged()
                        binding.recyclerView.smoothScrollToPosition(0)
                    }
                }
            })
    }

    private fun subscribeToNewRatings() {
        rateBusListener =  RxBus.listern(NewRateEvent::class.java).subscribe {
            saveRate(it.rate)
        }
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

    override fun onDestroyView() {
        rateBusListener.dispose()
        ratesSubscription?.remove()
        super.onDestroyView()
    }
}