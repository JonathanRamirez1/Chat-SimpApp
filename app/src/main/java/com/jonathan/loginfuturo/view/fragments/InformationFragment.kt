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
import com.jonathan.loginfuturo.providers.AuthProvider
import com.jonathan.loginfuturo.providers.UserProvider
import com.squareup.picasso.Picasso
import io.reactivex.disposables.Disposable

class InformationFragment : Fragment() {

    private lateinit var binding: FragmentInformationBinding
    private  lateinit var firebaseUser: FirebaseUser
    private lateinit var chatDataBaseReference: CollectionReference
    private lateinit var infoBusListener: Disposable
    private lateinit var authProvider: AuthProvider
    private lateinit var userProvider: UserProvider

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val fireBaseStore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var chatSubscription: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_information, container, false)

        authProvider = AuthProvider()
        userProvider = UserProvider()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUPCurrentUser()
        setUpCurrentUserInfoUI()

        //Total Messages Event Bus + Reactive Style
        subscribeTotalMessagesEventBusReactiveStyle()
    }

    /** SI EL USUAROI ESTA LOGGEADO LO DA Y SINO LO MANDA A LA PANTALLA DE LOGGIN O CUALQUIER OTRA ACCION
     * ESTO TAMBIEN SIRVE PARA EVITAR EL NullPointException DEL firebaseAuth.currentUser!! ESTO (setUPCurrentUser()) DEBE LLAMARSE
     * ANTES DE USARSE EL firebaseAuth.currentUser!! **/
    private fun setUPCurrentUser() {
            firebaseUser = firebaseAuth.currentUser!!
    }

    /**Si el usuario inició sesión con google me trae los datos que solicito, y si inicio sesion con
     * correo y contraseña se hace una consulta (get) para obtener lo que requiero (VER ELSE)**/
    private fun setUpCurrentUserInfoUI() {
        val id: String = firebaseAuth.currentUser!!.uid
            binding.textViewInfoEmail.text = firebaseUser.email
            binding.textViewInfoName.text = firebaseUser.displayName
        if (firebaseUser.displayName != null) {
            firebaseUser.displayName
        } else {
            userProvider.getCollection(id).addOnSuccessListener { task ->
                binding.textViewInfoName.text = task.get("username").toString()
            }
        }
            firebaseUser.photoUrl?.let {
                Picasso.get().load(firebaseUser.photoUrl).resize(300, 300)
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