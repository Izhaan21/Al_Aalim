package com.example.al_aalim.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.R
import com.example.al_aalim.model.RamadanDay
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation

class RamadanDayAdapter(
    private val days: List<RamadanDay>,
    private val onDayClick: (RamadanDay) -> Unit
) : RecyclerView.Adapter<RamadanDayAdapter.RamadanDayViewHolder>() {

    inner class RamadanDayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardDay: CardView = itemView.findViewById(R.id.card_day)
        val dayContainer: LinearLayout = itemView.findViewById(R.id.day_container)
        val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
        val tvSuhoorTime: TextView = itemView.findViewById(R.id.tv_suhoor_time)
        val tvIftarTime: TextView = itemView.findViewById(R.id.tv_iftar_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RamadanDayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ramadan_day, parent, false)
        return RamadanDayViewHolder(view)
    }

    override fun onBindViewHolder(holder: RamadanDayViewHolder, position: Int) {
        val day = days[position]
        
        // Set day data
        holder.tvDayNumber.text = day.dayNumber.toString()
        holder.tvSuhoorTime.text = day.suhoorTime
        holder.tvIftarTime.text = day.iftarTime
        
        // Highlight current day with gold border
        if (day.isToday) {
            holder.dayContainer.setBackgroundResource(R.drawable.input_field_glass)
            // Add gold border effect by setting a stroke (you can create a custom drawable for this)
            holder.cardDay.setCardBackgroundColor(Color.parseColor("#FFD700"))
            holder.cardDay.cardElevation = 4f
        } else {
            holder.dayContainer.setBackgroundResource(R.drawable.input_field_glass)
            holder.cardDay.setCardBackgroundColor(Color.TRANSPARENT)
            holder.cardDay.cardElevation = 0f
        }
        
        // Click listener with animation
        holder.itemView.setOnClickWithAnimation {
            onDayClick(day)
        }
    }

    override fun getItemCount(): Int = days.size
}
