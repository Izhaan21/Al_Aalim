package com.example.al_aalim.adapter

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.al_aalim.R
import com.example.al_aalim.model.Reciter

class ReciterAdapter(
    private var reciters: List<Reciter>,
    private var selectedReciterId: String? = null,
    private val onReciterSelected: (Reciter) -> Unit
) : RecyclerView.Adapter<ReciterAdapter.ReciterViewHolder>() {

    private var filteredReciters: List<Reciter> = reciters

    class ReciterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout    = view.findViewById(R.id.reciter_container)
        val photoLayoutContainer: View = view.findViewById(R.id.photo_layout_container)
        val photo: ImageView           = view.findViewById(R.id.reciter_photo)
        val nameEnglish: TextView      = view.findViewById(R.id.reciter_name_english)
        val country: TextView          = view.findViewById(R.id.reciter_country)
        val selectionIndicator: ImageView = view.findViewById(R.id.selection_indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReciterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reciter, parent, false)
        return ReciterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReciterViewHolder, position: Int) {
        val reciter = filteredReciters[position]
        val ctx     = holder.itemView.context

        holder.nameEnglish.text = reciter.nameEnglish
        holder.country.text     = reciter.country

        // Hide photo container entirely if offline
        if (reciter.photoUrl.isNotEmpty() && isOnline(ctx)) {
            holder.photoLayoutContainer.visibility = View.VISIBLE
            holder.photo.load(reciter.photoUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_account)
                error(R.drawable.ic_account)
                transformations(CircleCropTransformation())
            }
        } else {
            // Offline or no URL — hide the entire photo container
            holder.photoLayoutContainer.visibility = View.GONE
        }

        // Selection indicator
        holder.selectionIndicator.visibility =
            if (reciter.id == selectedReciterId) View.VISIBLE else View.GONE

        // Click
        holder.container.setOnClickListener {
            val oldSelectedId = selectedReciterId
            selectedReciterId = reciter.id
            notifyItemChanged(filteredReciters.indexOfFirst { it.id == oldSelectedId })
            notifyItemChanged(position)
            onReciterSelected(reciter)
        }
    }

    override fun getItemCount() = filteredReciters.size

    fun filter(query: String) {
        filteredReciters = if (query.isEmpty()) reciters
        else reciters.filter {
            it.nameEnglish.contains(query, ignoreCase = true) ||
            it.country.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }

    fun updateSelection(reciterId: String) {
        val oldSelectedId = selectedReciterId
        selectedReciterId = reciterId
        notifyItemChanged(filteredReciters.indexOfFirst { it.id == oldSelectedId })
        notifyItemChanged(filteredReciters.indexOfFirst { it.id == reciterId })
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
