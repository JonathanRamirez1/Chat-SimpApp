package com.jonathan.loginfuturo.dialogues

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.Utils.RxBus
import com.jonathan.loginfuturo.databinding.DialogRateBinding
import com.jonathan.loginfuturo.model.NewRateEvent
import com.jonathan.loginfuturo.model.Rate
import java.util.*


class RateDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = activity
        val binding: DialogRateBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.dialog_rate, null, false)

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
