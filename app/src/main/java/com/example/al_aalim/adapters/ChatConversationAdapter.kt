package com.example.al_aalim.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.R
import com.example.al_aalim.models.ChatConversation

/**
 * Adapter for chat conversation list in navigation drawer
 */
class ChatConversationAdapter(
    private val onConversationClick: (ChatConversation) -> Unit,
    private val onEditClick: (ChatConversation) -> Unit,
    private val onDeleteClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<ChatConversationAdapter.ConversationViewHolder>() {

    private val conversations = mutableListOf<ChatConversation>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversation, parent, false)
        return ConversationViewHolder(view, onConversationClick, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    /**
     * Update conversation list
     */
    fun submitList(newConversations: List<ChatConversation>) {
        conversations.clear()
        conversations.addAll(newConversations)
        notifyDataSetChanged()
    }

    /**
     * Filter conversations by query
     */
    fun filter(query: String, allConversations: List<ChatConversation>) {
        val filtered = if (query.isEmpty()) {
            allConversations
        } else {
            allConversations.filter {
                it.title.contains(query, ignoreCase = true)
            }
        }
        submitList(filtered)
    }

    class ConversationViewHolder(
        itemView: View,
        private val onConversationClick: (ChatConversation) -> Unit,
        private val onEditClick: (ChatConversation) -> Unit,
        private val onDeleteClick: (ChatConversation) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.tv_conversation_title)
        private val editButton: View = itemView.findViewById(R.id.btn_edit_conversation)
        private val deleteButton: View = itemView.findViewById(R.id.btn_delete_conversation)
        private var currentConversation: ChatConversation? = null

        init {
            itemView.setOnClickListener {
                currentConversation?.let { onConversationClick(it) }
            }
            
            editButton.setOnClickListener {
                it.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
                    it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    currentConversation?.let { conv -> onEditClick(conv) }
                }
            }

            deleteButton.setOnClickListener {
                it.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
                    it.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                    currentConversation?.let { conv -> onDeleteClick(conv) }
                }
            }
        }

        fun bind(conversation: ChatConversation) {
            currentConversation = conversation
            titleTextView.text = conversation.title
        }
    }
}
