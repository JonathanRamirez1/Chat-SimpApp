package com.jonathan.loginfuturo.ui.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jonathan.loginfuturo.databinding.FragmentContainerChatBinding

class ContainerChatFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
       val binding = FragmentContainerChatBinding.inflate(inflater, container, false)
        return binding.root
    }
}