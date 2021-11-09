package com.jonathan.chatsimpapp.core

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.objects.RxBus
import com.jonathan.chatsimpapp.databinding.DialogRateBinding
import com.jonathan.chatsimpapp.data.model.NewRateEvent
import com.jonathan.chatsimpapp.data.model.Rate
import java.util.*

class RateDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity
        val binding: DialogRateBinding = DialogRateBinding.inflate(
            layoutInflater, null, false)

        return  AlertDialog.Builder(activity)
            .setTitle(getString(R.string.alert_dialog_title))
            .setView(binding.root)
            .setPositiveButton(getString(R.string.alert_dialog_ok)) { _, _ ->
                val textRate = binding.editTextRateFeedback.text.toString()
                if (textRate.isNotEmpty()) {
                    val imageUrl = FirebaseAuth.getInstance().currentUser!!.photoUrl?.toString() ?: run { "" }
                    val rate = Rate(textRate, binding.ratingBarFeedback.rating, Date(), imageUrl)
                    RxBus.publish(NewRateEvent(rate))
                }
            }
            .setNegativeButton(getString(R.string.alert_dialog_cancel)) { _, _ ->
                Toast.makeText(context, "Pressed Cancel", Toast.LENGTH_SHORT).show()
            }
            .create()
    }
}
