package com.jonathan.loginfuturo.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Utils.RxBus
import com.jonathan.loginfuturo.model.Rate
import com.jonathan.loginfuturo.dialogues.RateDialog
import com.jonathan.loginfuturo.databinding.FragmentRatesBinding
import com.jonathan.loginfuturo.model.NewRateEvent
import com.jonathan.loginfuturo.view.adapters.RatesAdapter
import io.reactivex.disposables.Disposable
import java.util.EventListener
import kotlin.collections.ArrayList


class RatesFragment : Fragment() {

    private lateinit var binding: FragmentRatesBinding
    private lateinit var ratesAdapter: RatesAdapter
    private lateinit var ratesDataBaseReference: CollectionReference
    private lateinit var rateBusListener: Disposable
    private  var firebaseUser: FirebaseUser? = null

    private val ratesList: ArrayList<Rate> = ArrayList()
    private var ratesSubscription: ListenerRegistration? = null
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_rates, container, false)


        setUpRateDataBase()
        setUPCurrentUser()
        setUpRecyclerView()
        sepUpFab()
        subscribeToRatings()
        subscribeToNewRatings()


        return  binding.root
    }


    /** CUANDO SE AGREGA ALGUN VALOR VERIFICA SI EXISTE, DE EXISTIR LA AÑADE Y SINO LA CREA**/

    private fun setUpRateDataBase() {
        ratesDataBaseReference = fireBaseStore.collection("rates")
    }

    /** SI EL USUAROI ESTA LOGGEADO LO DA Y SINO LO MANDA A LA PANTALLA DE LOGGIN O CUALQUIER OTRA ACCION**/

    private fun setUPCurrentUser() {
        if (firebaseUser != null) {
            firebaseUser.let { firebaseAuth.currentUser!! }
        }
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

    override fun onDestroyView() {
        rateBusListener.dispose()
        ratesSubscription?.remove()
        super.onDestroyView()
    }
}