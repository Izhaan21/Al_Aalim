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
    private val onConversationLongClick: (ChatConversation) -> Unit
) : RecyclerView.Adapter<ChatConversationAdapter.ConversationViewHolder>() {

    private val conversations = mutableListOf<ChatConversation>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_conversation, parent, false)
        return ConversationViewHolder(view, onConversationClick, onConversationLongClick)
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
        private val onConversationLongClick: (ChatConversation) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val titleTextView: TextView = itemView.findViewById(R.id.tv_conversation_title)
        private var currentConversation: ChatConversation? = null

        init {
            itemView.setOnClickListener {
                currentConversation?.let { onConversationClick(it) }
            }
            
            itemView.setOnLongClickListener {
                currentConversation?.let { onConversationLongClick(it) }
                true
            }
        }

        fun bind(conversation: ChatConversation) {
            currentConversation = conversation
            titleTextView.text = conversation.title
        }
    }
}
