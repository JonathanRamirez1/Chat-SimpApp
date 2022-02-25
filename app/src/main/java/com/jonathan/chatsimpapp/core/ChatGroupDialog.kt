package com.jonathan.chatsimpapp.core

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.jonathan.chatsimpapp.databinding.DialogGroupChatBinding

class ChatGroupDialog: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity
        val binding: DialogGroupChatBinding = DialogGroupChatBinding.inflate(
            layoutInflater, null, false)

        return AlertDialog.Builder(activity)
            .setTitle("Datos del Grupo")
            .setView(binding.root)
            .setCancelable(false)
            .setPositiveButton("Crear Grupo") { _, _ ->
                val name = binding.editTextName.text.toString()
                val description = binding.editTextDescription.text.toString()
                val imageGroup = binding.circleImageViewChatGroup
                if (name.isNotEmpty() && description.isNotEmpty()) {

                } else {

                }
            }
            .setNegativeButton("Cancelar") { _, _ ->

            }
            .create()
    }
}