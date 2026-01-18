package com.example.al_aalim.models

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Data class representing a chat message
 * Designed for Firebase Realtime Database storage
 */
@IgnoreExtraProperties
data class ChatMessage(
    val id: String = "",
    val userId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val attachments: List<Attachment> = emptyList(),
    val isUser: Boolean = true
) {
    /**
     * Convert to HashMap for Firebase
     */
    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "message" to message,
            "timestamp" to timestamp,
            "attachments" to attachments.map { it.toMap() },
            "isUser" to isUser
        )
    }
}
