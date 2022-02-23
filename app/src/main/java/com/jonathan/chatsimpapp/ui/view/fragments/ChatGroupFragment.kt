package com.jonathan.chatsimpapp.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.ext.createFactory
import com.jonathan.chatsimpapp.databinding.FragmentChatGroupBinding
import com.jonathan.chatsimpapp.ui.viewmodels.ChatGroupViewModel


class ChatGroupFragment : Fragment() {

    private lateinit var binding: FragmentChatGroupBinding

    private val chatGroupViewModel by viewModels<ChatGroupViewModel> {
        ChatGroupViewModel().createFactory()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChatGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun sendMessageGroup() {
        binding.buttonSend.setOnClickListener {
            chatGroupViewModel.checkChatGroupExist()
        }
    }

}