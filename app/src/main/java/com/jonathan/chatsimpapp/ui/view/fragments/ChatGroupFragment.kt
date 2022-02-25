package com.jonathan.chatsimpapp.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.databinding.FragmentChatGroupBinding
import com.jonathan.chatsimpapp.ui.view.adapters.MessageAdapter
import com.jonathan.chatsimpapp.ui.viewmodels.ChatGroupViewModel


class ChatGroupFragment : Fragment() {

    private lateinit var binding: FragmentChatGroupBinding

    private var messageAdapter: MessageAdapter? = null

    private val chatGroupViewModel by viewModels<ChatGroupViewModel> {
        ChatGroupViewModel().createFactory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChatGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
        sendMessageGroup()
    }

    private fun setObservers() {

        chatGroupViewModel.editTextMessage.observe(viewLifecycleOwner) { text ->
            binding.editTextMessage.setText(text)
        }

        chatGroupViewModel.isNotifyMessage.observe(viewLifecycleOwner) { message ->
            if (message == true) {
                messageAdapter?.notifyDataSetChanged()
            }
        }
    }

    private fun sendMessageGroup() {
        binding.buttonSend.setOnClickListener {
            saveMessageGroup()
            chatGroupViewModel.checkChatGroupExist()
        }
    }

    private fun saveMessageGroup() {
        val messageGroup = binding.editTextMessage.text.toString()
        chatGroupViewModel.message(messageGroup)
    }

}