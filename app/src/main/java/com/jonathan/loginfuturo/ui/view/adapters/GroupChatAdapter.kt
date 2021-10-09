package com.jonathan.loginfuturo.ui.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jonathan.loginfuturo.core.Constants.GLOBAL_MESSAGE
import com.jonathan.loginfuturo.core.Constants.MY_MESSAGE
import com.jonathan.loginfuturo.data.model.MessageModel
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.core.CircleTransform
import com.jonathan.loginfuturo.data.model.providers.AuthProvider
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_chat_item_left.view.*
import kotlinx.android.synthetic.main.fragment_chat_item_right.view.*

class GroupChatAdapter (val items : List<MessageModel>, val userId : String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val layoutRight = R.layout.fragment_chat_item_right
    private val layoutLeft = R.layout.fragment_chat_item_left
    private val firebaseAuth = AuthProvider()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            MY_MESSAGE -> ViewHolderRight(layoutInflater.inflate(layoutRight, parent, false))
            else -> ViewHolderLeft(layoutInflater.inflate(layoutLeft, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            MY_MESSAGE -> (holder as ViewHolderRight).bind(items[position])
            GLOBAL_MESSAGE -> (holder as ViewHolderLeft).bind(items[position])
        }
    }

    /** Se usa este metodo cuando hay diferente tipo de item **/

    override fun getItemViewType(position: Int): Int  =
        if (firebaseAuth.getUid().let { items[position].getIdEmisor() } == userId) MY_MESSAGE else GLOBAL_MESSAGE

    class ViewHolderRight(itemView : View) : RecyclerView.ViewHolder(itemView) {
        fun bind(messageModel: MessageModel) = with(itemView) {
            textViewMessageRight.text = messageModel.getMessage()
            textViewTimeRight.text = messageModel.getTimeStamp().toString()
            if (messageModel.getProfileImageUrl().isEmpty()) {
                Picasso.get().load(R.drawable.person).resize(100, 100)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(imageViewProfileRight)
            } else {
                Picasso.get().load(messageModel.getProfileImageUrl()).resize(100, 100)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(imageViewProfileRight)
            }
        }
    }

    class ViewHolderLeft(itemView : View) : RecyclerView.ViewHolder(itemView) {

        fun bind(messageModel: MessageModel) = with(itemView) {
            textViewMessageLeft.text = messageModel.getMessage()
            textViewTimeLeft.text =  messageModel.getTimeStamp().toString()
            if (messageModel.getProfileImageUrl().isEmpty()) {
                Picasso.get().load(R.drawable.person).resize(100, 100)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(imageViewProfileLeft)
            } else {
                Picasso.get().load(messageModel.getProfileImageUrl()).resize(100, 100)
                    .centerCrop()
                    .transform(CircleTransform())
                    .into(imageViewProfileLeft)
            }
        }
    }
}