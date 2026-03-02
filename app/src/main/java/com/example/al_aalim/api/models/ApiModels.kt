package com.example.al_aalim.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response model from the API
 */
data class ApiResponse(
    @SerializedName("question")
    val question: String,
    
    @SerializedName("answer")
    val answer: String,
    
    @SerializedName("sources")
    val sources: List<String> = emptyList()
)

/**
 * Response model for starting a conversation
 */
data class ConversationStartResponse(
    @SerializedName("conversation_id")
    val conversationId: String,
    
    @SerializedName("message")
    val message: String
)

/**
 * Sealed class for API results
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}
