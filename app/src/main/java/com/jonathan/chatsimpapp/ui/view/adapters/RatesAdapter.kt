package com.jonathan.chatsimpapp.ui.view.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jonathan.chatsimpapp.R
import com.jonathan.chatsimpapp.core.CircleTransform
import com.jonathan.chatsimpapp.data.model.Rate
import com.jonathan.chatsimpapp.databinding.ItemRatesBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat

class RatesAdapter(private  val items : List<Rate>) : RecyclerView.Adapter<RatesAdapter.RatesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :  RatesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemRatesBinding.inflate(layoutInflater, parent, false)
        return RatesViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RatesViewHolder, position: Int) {
        return holder.bind(items[position])
    }

    class RatesViewHolder(val binding: ItemRatesBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rate: Rate) = with(binding) {
            binding.textViewRate.text = rate.text
            binding.textViewStar.text = rate.rate.toString()
            binding.textViewCalendar.text = SimpleDateFormat("dd MMMM, yyyy").format(rate.createAt)
            if (rate.profileImageUrl.isEmpty()) {
                Picasso.get().load(R.drawable.person).resize(100, 100)
                    .centerCrop().transform(CircleTransform())
                    .into(binding.imageViewProfile)
            } else {
                Picasso.get().load(rate.profileImageUrl).resize(100, 100)
                    .centerCrop().transform(CircleTransform())
                    .into(binding.imageViewProfile)
            }
        }
    }
}