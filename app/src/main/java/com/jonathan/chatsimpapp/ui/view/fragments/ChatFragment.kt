package com.jonathan.chatsimpapp.ui.view.fragments

import android.app.Activity
import android.content.Context.*
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.*
import com.jonathan.chatsimpapp.databinding.FragmentChatBinding
import com.jonathan.chatsimpapp.data.model.MessageModel
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.ui.view.adapters.MessageAdapter
import com.jonathan.chatsimpapp.core.objects.RelativeTime
import com.jonathan.chatsimpapp.core.objects.UpdateStateHelper
import com.jonathan.chatsimpapp.core.WrapContentLinearLayoutManager
import android.net.ConnectivityManager
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.viewModels
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.core.firebase.MessageReceiver
import com.jonathan.chatsimpapp.ui.view.activities.HomeActivity
import com.jonathan.chatsimpapp.ui.viewmodels.ChatViewModel

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var wrapContentLinearLayoutManager: WrapContentLinearLayoutManager

    private var messageAdapter: MessageAdapter? = null
    private var messageReceiver: MessageReceiver? = null
    private var interstitial: InterstitialAd? = null

    private val chatViewModel by viewModels<ChatViewModel> {
        ChatViewModel().createFactory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()

        getIds()
        chatViewModel.getInfoUserForNotifications()
        sendMessage()
        launchRoomsFragment()
        chatViewModel.getUserInfo()
        chatViewModel.getStateUser()
        chatViewModel.createChatInRealTimeDB()
        chatViewModel.checkIsWriting()
        setWritingUser()
        initInterstitialAd()
        initListeners()
    }

    private fun setObservers() {

        chatViewModel.editTextMessage.observe(viewLifecycleOwner) { text ->
            binding.editTextMessage.setText(text)
        }

        chatViewModel.isNotifyMessage.observe(viewLifecycleOwner) { message ->
            if (message == true) {
                messageAdapter?.notifyDataSetChanged()
            }
        }

        chatViewModel.showToast.observe(viewLifecycleOwner) { toastMessage ->
            Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_SHORT).show()
        }

        chatViewModel.usernameChat.observe(viewLifecycleOwner) { usernameChat ->
            binding.textViewUsernameChat.text = usernameChat
        }

        chatViewModel.online.observe(viewLifecycleOwner) { online ->
            getConnectivity(online)
        }

        chatViewModel.isOffline.observe(viewLifecycleOwner) { offline ->
            if (offline == true) {
                binding.TextViewTimeChat.text = resources.getString(R.string.offline)
            }
        }

        chatViewModel.imageChat.observe(viewLifecycleOwner) { imageChat ->
            imageChat.into(binding.circleImageViewChat)
        }

        //TODO CAMBIAR COLOR
        chatViewModel.isWriting.observe(viewLifecycleOwner) { writing ->
            if (writing == true) {
                binding.TextViewWritingChat.visibility = View.VISIBLE
                binding.TextViewWritingChat.setTextColor(resources.getColor(R.color.colorGooglePlusDark))
                binding.TextViewTimeChat.visibility = View.GONE
            } else if (writing == false) {
                binding.TextViewWritingChat.visibility = View.GONE
                binding.TextViewTimeChat.visibility = View.VISIBLE
            }
        }
    }

    /**Recuperación de los ids enviados desde roomsAdapter**/
    private fun getIds() {
        chatViewModel.idUserEmisor = arguments?.getString("idEmisor").toString()
        chatViewModel.idUserReceptor = arguments?.getString("idReceptor").toString()
        chatViewModel.idChat = arguments?.getString("idChat").toString()
    }

    private fun getAllMessagesUser() {
        chatViewModel.chatModel.setId(chatViewModel.idUserEmisor + chatViewModel.idUserReceptor)
        chatViewModel.idChat = chatViewModel.chatModel.getId()
        val query: Query = chatViewModel.messageProvider.getMessageByChat(chatViewModel.idChat)
        val options = FirestoreRecyclerOptions.Builder<MessageModel>()
            .setQuery(query, MessageModel::class.java)
            .build()

        messageAdapter = MessageAdapter(options, context)
        binding.recyclerViewChat.adapter = messageAdapter
        wrapContentLinearLayoutManager = WrapContentLinearLayoutManager(context)
        wrapContentLinearLayoutManager.stackFromEnd = true
        binding.recyclerViewChat.layoutManager = wrapContentLinearLayoutManager
        messageAdapter?.startListening()
        messageAdapter?.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                chatViewModel.updateViewed()
                val numberMessage: Int = messageAdapter!!.itemCount
                val lastMessagePosition = wrapContentLinearLayoutManager.findLastCompletelyVisibleItemPosition()
                if (lastMessagePosition == -1 || (positionStart >= (numberMessage - 1) && lastMessagePosition == (positionStart - 1))) {
                    binding.recyclerViewChat.scrollToPosition(positionStart)
                }
            }
        })
    }

    private fun sendMessage() {
        binding.buttonSend.setOnClickListener {
            saveMessage()
            chatViewModel.checkChatExist()
        }
    }

    private fun saveMessage() {
        val messageText = binding.editTextMessage.text.toString()
        chatViewModel.message(messageText)
    }

    private fun launchRoomsFragment() {
        val goRooms = binding.buttonBackRooms

        goRooms.setOnClickListener {
            val intent = Intent(context, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            showAds()
            initInterstitialAd()
        }
    }

    private fun setWritingUser() {
        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.trim().isNotEmpty()) {
                    chatViewModel.updateWritingUser("true")
                } else {
                    chatViewModel.updateWritingUser("false")
                }
            }
        })
    }

    private fun initListeners() {
        interstitial?.fullScreenContentCallback = object : FullScreenContentCallback() {

            override fun onAdDismissedFullScreenContent() {}
            override fun onAdFailedToShowFullScreenContent(adError: AdError?) {}
            override fun onAdShowedFullScreenContent() {
                interstitial = null
            }
        }
    }

    //TODO CAMBIAR LOS IDS DE ADS(ANUNCIOS) EN EL XML Y EL MANIFEST CUANDO SE TERMINE LA APP, LOS ACTUALES SON DE PRUEBA (Ver strings o video de ari)
    private fun initInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            requireContext(),
            getString(R.string.test_interstitial),
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    interstitial = interstitialAd
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    interstitial = null
                }
            })
    }

    private fun showAds() {
        interstitial?.show(context as Activity)
    }

    private fun getConnectivity(online: Boolean) {
        val connectivityManager = context?.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected) {
            when (online) {
                true -> {
                    binding.TextViewTimeChat.text = resources.getString(R.string.online)
                }
                false -> {
                    val relativeTime = RelativeTime.getTimeAgo(chatViewModel.lastConnect, context)
                    binding.TextViewTimeChat.text = relativeTime
                }
            }
        } else {
            binding.TextViewTimeChat.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        if (messageAdapter != null) {
            messageAdapter?.startListening()
        }
        UpdateStateHelper.updateState("true", "false", requireContext())
        chatViewModel.setOnDisconnect()
        getAllMessagesUser()
    }

    override fun onResume() {
        super.onResume()
        (activity as AppCompatActivity?)?.supportActionBar?.hide()
        val navBar: BottomNavigationView? = activity?.findViewById(R.id.bottomNavigation)
        navBar?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        UpdateStateHelper.updateState("false", "false", requireContext())
    }

    override fun onStop() {
        super.onStop()
        messageAdapter?.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        chatViewModel.getUserInfoListener()?.remove()
        chatViewModel.getLastMessageListener()?.remove()
        messageReceiver?.getListenerLastMessageEmisor()?.remove()
    }
}