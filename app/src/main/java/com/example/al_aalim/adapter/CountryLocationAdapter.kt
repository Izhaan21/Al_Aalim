package com.example.al_aalim.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.R

class CountryLocationAdapter(
    var locations: MutableList<com.example.al_aalim.data.CountryLocation>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<CountryLocationAdapter.ViewHolder>() {
    
    private var selectedPos = -1
    
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFlag: TextView = view.findViewById(R.id.tv_flag)
        val tvLocationName: TextView = view.findViewById(R.id.tv_location_name)
        val ivSelected: ImageView = view.findViewById(R.id.iv_selected)
        val locationCard: CardView = view.findViewById(R.id.location_card)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_location, parent, false)
        return ViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val location = locations[position]
        holder.tvFlag.text = location.flag
        holder.tvLocationName.text = "${location.name}, ${location.capital}"
        
        // Show selection state
        if (selectedPos == position) {
            holder.ivSelected.visibility = View.VISIBLE
            holder.locationCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.background_light_gray)
            )
        } else {
            holder.ivSelected.visibility = View.GONE
            holder.locationCard.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
        }
        
        holder.itemView.setOnClickListener {
            val previousPos = selectedPos
            selectedPos = holder.adapterPosition
            notifyItemChanged(previousPos)
            notifyItemChanged(selectedPos)
            onItemClick(selectedPos)
        }
    }
    
    override fun getItemCount() = locations.size
    
    fun updateLocations(newLocations: MutableList<com.example.al_aalim.data.CountryLocation>) {
        locations = newLocations
        selectedPos = -1
        notifyDataSetChanged()
    }
}
