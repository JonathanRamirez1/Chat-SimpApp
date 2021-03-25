package com.jonathan.loginfuturo.view.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.TotalMessagesEvent
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.jonathan.loginfuturo.Utils.RxBus
import com.jonathan.loginfuturo.databinding.FragmentInformationBinding
import com.squareup.picasso.Picasso
import io.reactivex.disposables.Disposable

class InformationFragment : Fragment() {

    private lateinit var binding: FragmentInformationBinding
    private  var firebaseUser: FirebaseUser? = null
    private lateinit var chatDataBaseReference: CollectionReference
    private lateinit var infoBusListener: Disposable

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var chatSubscription: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? { binding = DataBindingUtil.inflate(inflater,
        R.layout.fragment_information, container, false)

        setUpChatDataBase()
       // setUPCurrentUser()
        setUpCurrentUserInfoUI()

        //Total Messages Event Bus + Reactive Style
        subscribeTotalMessagesEventBusReactiveStyle()

        return binding.root
    }

    /** CUANDO SE AGREGA ALGUN VALOR VERIFICA SI EXISTE, DE EXISTIR LA AÑADE Y SINO LA CREA**/

    private fun setUpChatDataBase() {
        chatDataBaseReference = fireBaseStore.collection("chat")
    }

    /** SI EL USUAROI ESTA LOGGEADO LO DA Y SINO LO MANDA A LA PANTALLA DE LOGGIN O CUALQUIER OTRA ACCION**/

    private fun setUPCurrentUser() {
        if (firebaseUser != null) {
            firebaseUser?.let { firebaseAuth.currentUser!! }
        }
    }


    private fun setUpCurrentUserInfoUI() {
        setUPCurrentUser()
            binding.textViewInfoEmail.text = firebaseUser?.email
            binding.textViewInfoName.text = firebaseUser?.displayName?.let { firebaseUser?.displayName }
                    ?: run { getString(R.string.info_no_name) }
            firebaseUser?.photoUrl?.let {
                Picasso.get().load(firebaseUser?.photoUrl).resize(300, 300)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(binding.imageViewInfoAvatar)
            } ?: run {
                Picasso.get().load(R.drawable.person).resize(300, 300)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(binding.imageViewInfoAvatar)
            }
    }


    @SuppressLint("CheckResult")
    /** Metodo para hacer programacion reactiva, se necesita crear una data class
     * (TotalMessagesEvent) por cada evento que se quiere escuchar**/
    private fun subscribeTotalMessagesEventBusReactiveStyle() {
        infoBusListener = RxBus.listern(TotalMessagesEvent::class.java).subscribe {
            binding.textViewInfoTotalMessages.text = "${it.total}"
        }
    }

    override fun onDestroyView() {
        infoBusListener.dispose()
        chatSubscription?.remove()
        super.onDestroyView()
    }
}

/*  //TODO CAMBIAR NOMBRE DE ESTE METODO
    private fun subscribeTotalMessagesFirebaseStyle() {
        chatSubscription = chatDataBaseReference.addSnapshotListener(object : EventListener, com.google.firebase.firestore.EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?, exception: FirebaseFirestoreException?) {
                exception?.let {
                    Toast.makeText(context, "Exception!", Toast.LENGTH_SHORT).show()
                    return
                }

                querySnapshot?.let {
                    _view
                        .textViewInfoTotalMessages.text = "${it.size()}"
                }
            }
        })
    }*/