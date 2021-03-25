package com.jonathan.loginfuturo.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jonathan.loginfuturo.R
import com.jonathan.loginfuturo.model.Rate
import com.jonathan.loginfuturo.Utils.CircleTransform
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_rates_item.view.*
import java.text.SimpleDateFormat

class RatesAdapter(private  val items : List<Rate>) : RecyclerView.Adapter<RatesAdapter.RatesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) :  RatesViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return RatesViewHolder(layoutInflater.inflate(R.layout.fragment_rates_item, parent, false))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: RatesViewHolder, position: Int) {
        return holder.bind(items[position])
    }

    class RatesViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        fun bind(rate: Rate) = with(itemView) {
            textViewRate.text = rate.text
            textViewStar.text = rate.rate.toString()
            textViewCalendar.text = SimpleDateFormat("dd MMMM, yyyy").format(rate.createAt)
            if (rate.profileImageUrl.isEmpty()) {
                Picasso.get().load(R.drawable.person).resize(100, 100)
                    .centerCrop().transform(CircleTransform())
                    .into(imageViewProfile)
            } else {
                Picasso.get().load(rate.profileImageUrl).resize(100, 100)
                    .centerCrop().transform(CircleTransform())
                    .into(imageViewProfile)
            }
        }
    }
}