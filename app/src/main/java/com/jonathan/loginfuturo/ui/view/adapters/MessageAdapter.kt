package com.jonathan.loginfuturo.ui.view.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.RelativeTime
import com.jonathan.loginfuturo.databinding.CardViewMessageBinding
import com.jonathan.loginfuturo.data.model.MessageModel
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.jonathan.loginfuturo.data.model.providers.UserProvider
import kotlinx.android.synthetic.main.card_view_message.view.*

class MessageAdapter(options: FirestoreRecyclerOptions<MessageModel>) : FirestoreRecyclerAdapter<MessageModel, MessageAdapter.MessageHolder>(options) {

    private lateinit var userProvider: UserProvider
    private lateinit var authProvider: AuthProvider

    private var context: Context? = null

    constructor(options: FirestoreRecyclerOptions<MessageModel>, context: Context?) : this(options) {
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

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onBindViewHolder(holder: MessageHolder, position: Int, messageModel: MessageModel) {
        val document = snapshots.getSnapshot(position)
        val messageId = document.id

        holder.render(messageModel)

        if (messageModel.getIdEmisor() == authProvider.getUid()) {
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
        if (messageModel.getViewed()) {
            holder.binding.imageViewViewedMessage.setImageResource(R.drawable.icon_check_blue)
        } else {
            holder.binding.imageViewViewedMessage.setImageResource(R.drawable.icon_check_grey)
        }
    }

    inner class MessageHolder(val binding: CardViewMessageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun render(messageModel: MessageModel) {
            binding.textViewMessage.text = messageModel.getMessage()
            val relativeTime = RelativeTime.timeFormatAMPM(messageModel.getTimeStamp(), context)
            binding.textViewDateMessage.text = relativeTime
        }
    }
}



