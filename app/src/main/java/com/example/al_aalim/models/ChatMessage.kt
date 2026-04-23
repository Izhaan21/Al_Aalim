package com.example.al_aalim.models

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName

/**
 * Data class representing a chat message
 * Designed for Firebase Realtime Database storage
 */
@androidx.annotation.Keep
@IgnoreExtraProperties
data class ChatMessage(
    var id: String = "",
    var userId: String = "",
    var message: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var attachments: List<Attachment> = emptyList(),
    @get:PropertyName("isUser")
    @set:PropertyName("isUser")
    var isUser: Boolean = true
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
