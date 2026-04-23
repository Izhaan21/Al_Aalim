package com.example.al_aalim.models

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Data class representing a chat conversation session
 */
@androidx.annotation.Keep
@IgnoreExtraProperties
data class ChatConversation(
    val id: String = "",
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messageCount: Int = 0
) {
    /**
     * Convert to HashMap for Firebase
     */
    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "title" to title,
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "messageCount" to messageCount
        )
    }
}
