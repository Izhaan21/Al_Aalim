package com.example.al_aalim.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.R
import com.example.al_aalim.model.Language

class LanguageAdapter(
    private var languages: List<Language>,
    private val onLanguageSelected: (Language) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    private var selectedLanguageCode: String? = null

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: LinearLayout = itemView.findViewById(R.id.language_item_container)
        val checkIcon: ImageView = itemView.findViewById(R.id.check_icon)
        val nativeName: TextView = itemView.findViewById(R.id.native_name)
        val englishName: TextView = itemView.findViewById(R.id.english_name)
        val flagIcon: ImageView = itemView.findViewById(R.id.flag_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        
        holder.nativeName.text = language.nativeName
        holder.englishName.text = language.englishName
        holder.flagIcon.setImageResource(language.flagResId)

        val isSelected = language.code == selectedLanguageCode
        if (isSelected) {
            holder.container.setBackgroundResource(R.drawable.language_item_glass_selected)
            holder.checkIcon.visibility = View.VISIBLE
        } else {
            holder.container.setBackgroundResource(R.drawable.language_item_glass_unselected)
            holder.checkIcon.visibility = View.INVISIBLE
        }

        holder.container.setOnClickListener {
            val previousSelected = selectedLanguageCode
            selectedLanguageCode = language.code
            onLanguageSelected(language)
            
            // Refresh previous and current selections
            if (previousSelected != null) {
                val prevIndex = languages.indexOfFirst { it.code == previousSelected }
                if (prevIndex != -1) notifyItemChanged(prevIndex)
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = languages.size

    fun updateLanguages(newLanguages: List<Language>) {
        languages = newLanguages
        notifyDataSetChanged()
    }

    fun setSelectedLanguage(code: String) {
        selectedLanguageCode = code
        notifyDataSetChanged()
    }
}
