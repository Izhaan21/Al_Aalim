package com.example.al_aalim.models

import com.google.firebase.database.IgnoreExtraProperties

/**
 * Data class representing a file attachment (image, video, or document)
 */
@androidx.annotation.Keep
@IgnoreExtraProperties
data class Attachment(
    val type: String = "", // "image", "video", "document"
    val url: String = "", // Firebase Storage download URL
    val fileName: String = "",
    val fileSize: Long = 0,
    val mimeType: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to HashMap for Firebase
     */
    fun toMap(): Map<String, Any?> {
        return hashMapOf(
            "type" to type,
            "url" to url,
            "fileName" to fileName,
            "fileSize" to fileSize,
            "mimeType" to mimeType,
            "uploadedAt" to uploadedAt
        )
    }
}
