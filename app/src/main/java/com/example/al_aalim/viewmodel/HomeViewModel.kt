package com.example.al_aalim.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.al_aalim.models.Attachment
import com.example.al_aalim.models.ChatMessage
import com.example.al_aalim.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class HomeViewModel(
    private val chatRepository: ChatRepository,
    private val appContext: Context
) : ViewModel() {

    private var activeConversationId: String? = null

    // UI State exposing the current chat messages
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    // State to track if the AI is currently typing (waiting for API response)
    private val _isAITyping = MutableStateFlow(false)
    val isAITyping: StateFlow<Boolean> = _isAITyping.asStateFlow()
    
    // State for error handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun setActiveConversation(conversationId: String?) {
        activeConversationId = conversationId
        if (conversationId != null) {
            loadConversationMessages(conversationId)
        } else {
            _chatMessages.value = emptyList()
            com.example.al_aalim.utils.GeminiService.clearHistory()
        }
    }

    fun getActiveConversationId(): String? = activeConversationId

    fun loadConversationMessages(conversationId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                chatRepository.getMessagesForConversation(conversationId)
            }
            if (result.isSuccess) {
                val messages = result.getOrNull() ?: emptyList()
                val sorted = messages.sortedBy { it.timestamp }
                _chatMessages.value = sorted
                // Fix 2/8: Populate Gemini history ONCE when a conversation is loaded,
                // not before every individual send. This avoids redundant rebuilds.
                com.example.al_aalim.utils.GeminiService.setChatHistory(sorted)
            } else {
                _errorMessage.value = "Failed to load messages."
            }
        }
    }

    // Fix 6: onRefreshDrawer callback lets ContainerActivity refresh the conversation
    // list in the drawer immediately after a new chat is created — no stale history.
    fun sendMessage(
        message: String,
        fileUris: List<Uri>,
        onConversationCreated: (String) -> Unit,
        onRefreshDrawer: (() -> Unit)? = null
    ) {
        if (!com.example.al_aalim.utils.NetworkUtils.isNetworkAvailable(appContext)) {
            _errorMessage.value = "No internet connection. Please check your network settings."
            return
        }

        if (activeConversationId == null) {
            viewModelScope.launch {
                val title = if (message.isNotBlank()) {
                    com.example.al_aalim.utils.StringUtils.generateConversationTitle(message)
                } else {
                    "Photo Message"
                }
                val result = withContext(Dispatchers.IO) {
                    chatRepository.createConversation(title)
                }
                if (result.isSuccess) {
                    activeConversationId = result.getOrNull()
                    activeConversationId?.let { newId ->
                        onConversationCreated(newId)
                        onRefreshDrawer?.invoke() // Fix 6: refresh drawer immediately
                        sendMessageToConversation(message, fileUris)
                    }
                } else {
                    _errorMessage.value = "Failed to create conversation."
                }
            }
        } else {
            sendMessageToConversation(message, fileUris)
        }
    }

    private fun sendMessageToConversation(message: String, fileUris: List<Uri>) {
        val conversationId = activeConversationId ?: return

        viewModelScope.launch {
            // Build local Attachment objects from URIs so photos appear immediately
            val localAttachments = fileUris.map { uri ->
                Attachment(
                    type = "image",
                    url = uri.toString(),  // local content:// or file:// URI for preview
                    fileName = "",
                    fileSize = 0,
                    mimeType = "image/jpeg"
                )
            }

            // UUID guarantees uniqueness even if two messages are sent in the same millisecond.
            // We also capture the ID so the rollback below removes ONLY this specific message
            // rather than all optimistic messages — which would break concurrent sends.
            val optimisticId = "optimistic_${UUID.randomUUID()}"
            val optimisticMessage = ChatMessage(
                id = optimisticId,
                message = message,
                isUser = true,
                timestamp = System.currentTimeMillis(),
                attachments = localAttachments
            )
            _chatMessages.value = _chatMessages.value + optimisticMessage

            // Then persist to Firebase in the background
            val result = withContext(Dispatchers.IO) {
                chatRepository.sendMessage(
                    conversationId = conversationId,
                    messageText = message,
                    fileUris = fileUris,
                    onUploadProgress = { _, _ -> }
                )
            }

            if (result.isSuccess) {
                // Always trigger AI response for text and/or images
                if (message.isNotEmpty() || fileUris.isNotEmpty()) {
                    getAIResponse(message, conversationId, fileUris)
                }
            } else {
                // Remove ONLY this specific optimistic message on failure,
                // leaving any other in-flight messages untouched.
                _chatMessages.value = _chatMessages.value.filter { it.id != optimisticId }
                _errorMessage.value = "Failed to send message: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * Read image URIs and convert to base64 for Gemini vision API
     */
    private suspend fun readImagesAsBase64(fileUris: List<Uri>): List<Pair<String, String>> {
        return withContext(Dispatchers.IO) {
            fileUris.mapNotNull { uri ->
                try {
                    val inputStream = appContext.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    if (bytes != null) {
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        val mimeType = appContext.contentResolver.getType(uri) ?: "image/jpeg"
                        Pair(base64, mimeType)
                    } else null
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to read image: ${e.message}")
                    null
                }
            }
        }
    }

    private fun getAIResponse(userMessage: String, conversationId: String, fileUris: List<Uri> = emptyList()) {
        _isAITyping.value = true

        viewModelScope.launch {
            try {
                // Fix 2/8: setChatHistory is now called ONLY in loadConversationMessages (on load).
                // GeminiService maintains its own rolling history after each response via chatHistory.add().
                // Rebuilding it before every send is redundant and resets the rolling context.

                // Read images as base64 if any
                val imageDataList = if (fileUris.isNotEmpty()) {
                    readImagesAsBase64(fileUris)
                } else {
                    emptyList()
                }

                // Get response from Gemini (text + optional images)
                val result = com.example.al_aalim.utils.GeminiService.sendMessage(userMessage, imageDataList)
                
                if (result.isSuccess) {
                    val aiResponse = result.getOrNull() ?: "No response generated."

                    // Fix 3: Use UUID instead of currentTimeMillis to prevent ID collisions
                    // when two AI responses arrive within the same millisecond.
                    val aiMessage = ChatMessage(
                        id = "ai_${UUID.randomUUID()}",
                        message = aiResponse,
                        isUser = false,
                        timestamp = System.currentTimeMillis()
                    )
                    _chatMessages.value = _chatMessages.value + aiMessage
                    _isAITyping.value = false

                    // Save to Firebase in the background (non-blocking)
                    launch(Dispatchers.IO) {
                        chatRepository.saveAIMessage(conversationId, aiResponse)
                    }
                } else {
                    _isAITyping.value = false
                    val error = result.exceptionOrNull()?.message ?: "Unknown AI error"
                    _errorMessage.value = "Gemini Error: $error"
                }
            } catch (e: Exception) {
                _isAITyping.value = false
                _errorMessage.value = "An unexpected error occurred: ${e.message}"
            }
        }
    }


    fun clearError() {
        _errorMessage.value = null
    }

    fun deleteMessage(messageId: String) {
        val convId = activeConversationId ?: return

        // Fix 1: Remove optimistically from local state FIRST — instant UI response, no flicker.
        // If Firebase delete fails, we restore the message and show an error.
        val previousMessages = _chatMessages.value
        _chatMessages.value = previousMessages.filter { it.id != messageId }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                chatRepository.deleteMessage(convId, messageId)
            }
            if (!result.isSuccess) {
                // Restore on failure so user doesn't lose the message silently
                _chatMessages.value = previousMessages
                _errorMessage.value = "Failed to delete message."
            }
        }
    }
}
