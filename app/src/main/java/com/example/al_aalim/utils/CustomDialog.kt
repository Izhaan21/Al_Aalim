package com.example.al_aalim.utils

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.R

class CustomDialog(private val context: Context) {
    
    companion object {
        /**
         * Show an enhanced confirmation dialog with optional icon
         */
        fun showConfirmation(
            context: Context,
            title: String,
            message: String,
            confirmText: String = "Confirm",
            cancelText: String = "Cancel",
            icon: Int? = null,
            onConfirm: () -> Unit,
            onCancel: (() -> Unit)? = null
        ) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_confirmation)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val dialogIcon = dialog.findViewById<ImageView>(R.id.dialog_icon)
            val dialogTitle = dialog.findViewById<TextView>(R.id.dialog_title)
            val dialogMessage = dialog.findViewById<TextView>(R.id.dialog_message)
            val btnConfirm = dialog.findViewById<TextView>(R.id.dialog_btn_confirm)
            val btnCancel = dialog.findViewById<TextView>(R.id.dialog_btn_cancel)
            
            // Show icon if provided
            if (icon != null) {
                dialogIcon.visibility = View.VISIBLE
                dialogIcon.setImageResource(icon)
            } else {
                dialogIcon.visibility = View.GONE
            }
            
            dialogTitle.text = title
            dialogMessage.text = message
            btnConfirm.text = confirmText
            btnCancel.text = cancelText
            
            btnConfirm.setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }
            
            btnCancel.setOnClickListener {
                onCancel?.invoke()
                dialog.dismiss()
            }
            
            dialog.show()
        }
        
        /**
         * Show an enhanced selection list dialog with optional icon and subtitle
         */
        fun showSelectionList(
            context: Context,
            title: String,
            subtitle: String? = null,
            icon: Int? = null,
            options: List<String>,
            selectedIndex: Int = -1,
            onSelect: (Int, String) -> Unit
        ) {
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            dialog.setContentView(R.layout.dialog_selection_list)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            
            val dialogIcon = dialog.findViewById<ImageView>(R.id.dialog_icon)
            val dialogTitle = dialog.findViewById<TextView>(R.id.dialog_title)
            val dialogSubtitle = dialog.findViewById<TextView>(R.id.dialog_subtitle)
            val recyclerView = dialog.findViewById<RecyclerView>(R.id.dialog_list)
            val btnCancel = dialog.findViewById<TextView>(R.id.dialog_btn_cancel)
            val btnApply = dialog.findViewById<TextView>(R.id.dialog_btn_apply)
            
            // Show icon if provided
            if (icon != null) {
                dialogIcon.visibility = View.VISIBLE
                dialogIcon.setImageResource(icon)
            } else {
                dialogIcon.visibility = View.GONE
            }
            
            dialogTitle.text = title
            
            // Show subtitle if provided
            if (subtitle != null) {
                dialogSubtitle.visibility = View.VISIBLE
                dialogSubtitle.text = subtitle
            } else {
                dialogSubtitle.visibility = View.GONE
            }
            
            var currentSelection = selectedIndex
            val adapter = SelectionAdapter(options, selectedIndex) { position, _ ->
                currentSelection = position
            }
            
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter
            
            btnApply.setOnClickListener {
                if (currentSelection >= 0) {
                    onSelect(currentSelection, options[currentSelection])
                }
                dialog.dismiss()
            }
            
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            
            dialog.show()
        }
    }
    
    /**
     * Enhanced adapter for selection list items with new radio buttons
     */
    private class SelectionAdapter(
        private val options: List<String>,
        private var selectedPosition: Int,
        private val onItemClick: (Int, String) -> Unit
    ) : RecyclerView.Adapter<SelectionAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val radio: ImageView = view.findViewById(R.id.item_radio)
            val title: TextView = view.findViewById(R.id.item_title)
            val subtitle: TextView = view.findViewById(R.id.item_subtitle)
            val icon: ImageView = view.findViewById(R.id.item_icon)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.dialog_list_item, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val option = options[position]
            holder.title.text = option
            
            // Hide subtitle and icon (not used in simple string list)
            holder.subtitle.visibility = View.GONE
            holder.icon.visibility = View.GONE
            
            // Update radio button state with new drawables
            if (position == selectedPosition) {
                holder.radio.setImageResource(R.drawable.ic_radio_selected_gold)
            } else {
                holder.radio.setImageResource(R.drawable.ic_radio_unselected_gray)
            }
            
            holder.itemView.setOnClickListener {
                val oldPosition = selectedPosition
                selectedPosition = position
                notifyItemChanged(oldPosition)
                notifyItemChanged(selectedPosition)
                onItemClick(position, option)
            }
        }
        
        override fun getItemCount() = options.size
    }
}
