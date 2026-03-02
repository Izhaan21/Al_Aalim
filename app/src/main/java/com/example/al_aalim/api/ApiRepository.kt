package com.example.al_aalim.api

import com.example.al_aalim.api.models.ApiResponse
import com.example.al_aalim.api.models.ApiResult
import com.example.al_aalim.api.models.ConversationStartResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApiRepository(private val apiService: ApiService = RetrofitClient.apiService) {
    
    /**
     * Ask a question to the API
     * @param question The question text
     * @return ApiResult with the response or error
     */
    suspend fun askQuestion(question: String, conversationId: String? = null): ApiResult<ApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.askQuestion(question, conversationId)
                
                if (response.isSuccessful && response.body() != null) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(
                        message = "Error: ${response.code()} - ${response.message()}",
                        exception = null
                    )
                }
            } catch (e: Exception) {
                ApiResult.Error(
                    message = "Network error: ${e.localizedMessage ?: "Unknown error"}",
                    exception = e
                )
            }
        }
    }
    
    /**
     * Start a new conversation session
     */
    suspend fun startConversation(): ApiResult<ConversationStartResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.startConversation()
                
                if (response.isSuccessful && response.body() != null) {
                    ApiResult.Success(response.body()!!)
                } else {
                    ApiResult.Error(
                        message = "Error: ${response.code()} - ${response.message()}",
                        exception = null
                    )
                }
            } catch (e: Exception) {
                ApiResult.Error(
                    message = "Network error: ${e.localizedMessage ?: "Unknown error"}",
                    exception = e
                )
            }
        }
    }
}
