package com.jonathan.loginfuturo.ui.view.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.RelativeTime
import com.jonathan.loginfuturo.databinding.CardViewMessageBinding
import com.jonathan.loginfuturo.data.model.Message
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import kotlinx.android.synthetic.main.card_view_message.view.*

class MessageAdapter(options: FirestoreRecyclerOptions<Message>) : FirestoreRecyclerAdapter<Message, MessageAdapter.MessageHolder>(options) {

    private lateinit var navController: NavController
    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider
    private var bundle = Bundle()

    private var context: Context? = null

    constructor(options: FirestoreRecyclerOptions<Message>, context: Context?) : this(options) {
        super.updateOptions(options)
        this.context = context
        userProvider = UserProvider()
        authProvider = AuthProvider()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageAdapter.MessageHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = CardViewMessageBinding.inflate(layoutInflater, parent, false)
        return MessageHolder(binding)

    }

    override fun onBindViewHolder(holder: MessageHolder, position: Int, message: Message) {
        val document = snapshots.getSnapshot(position)
        val messageId = document.id

        //holder.bindingAdapterPosition
        holder.render(message)

        if (message.getIdEmisor() == authProvider.getUid()) {
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT)
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.setMargins(150, 0, 0, 0)
            holder.itemView.linearLayoutMessage.layoutParams = params
            holder.itemView.linearLayoutMessage.setPadding(40, 20, 40, 20)
            holder.itemView.linearLayoutMessage.background = context!!.resources.getDrawable(R.drawable.rounded_linear_layout)
            holder.itemView.imageViewViewedMessage.visibility = View.VISIBLE
        } else {
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.setMargins(0, 0, 150, 0)
            holder.itemView.linearLayoutMessage.layoutParams = params
            holder.itemView.linearLayoutMessage.setPadding(40, 20, 40, 20)
            holder.itemView.linearLayoutMessage.background = context!!.resources.getDrawable(R.drawable.rounded_linear_layout_grey)
            holder.itemView.imageViewViewedMessage.visibility = View.GONE
        }
        if (message.getViewed()) {
            holder.binding.imageViewViewedMessage.setImageResource(R.drawable.icon_check_blue)
        } else {
            holder.binding.imageViewViewedMessage.setImageResource(R.drawable.icon_check_grey)
        }
    }

    inner class MessageHolder(val binding: CardViewMessageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun render(message: Message) {
            binding.textViewMessage.text = message.getMessage()
            val relativeTime = RelativeTime.timeFormatAMPM(message.getTimeStamp(), context)
            binding.textViewDateMessage.text = relativeTime
        }
    }
}



