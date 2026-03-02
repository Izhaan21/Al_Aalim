package com.example.al_aalim.api

import com.example.al_aalim.api.models.ApiResponse
import com.example.al_aalim.api.models.ConversationStartResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    /**
     * Start a new conversation
     * POST /api/conversation/start
     */
    @POST("api/conversation/start")
    suspend fun startConversation(): Response<ConversationStartResponse>

    /**
     * Ask a question to the API
     * GET /api/ask?text={question}&conversation_id={id}
     */
    @GET("api/ask")
    suspend fun askQuestion(
        @Query("text") question: String,
        @Query("conversation_id") conversationId: String? = null
    ): Response<ApiResponse>
}
