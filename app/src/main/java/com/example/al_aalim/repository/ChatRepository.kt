package com.example.al_aalim.repository

import android.content.Context
import android.net.Uri
import com.example.al_aalim.config.FirebaseConfig
import com.example.al_aalim.models.Attachment
import com.example.al_aalim.models.ChatConversation
import com.example.al_aalim.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for Firebase Realtime Database operations
 */
class ChatRepository(context: Context) {
    
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance().apply {
        if (FirebaseConfig.USE_EMULATOR) {
            useEmulator(FirebaseConfig.EMULATOR_HOST, FirebaseConfig.DATABASE_EMULATOR_PORT)
        }
    }
    
    private val messagesRef: DatabaseReference = database.getReference(FirebaseConfig.MESSAGES_PATH)
    private val conversationsRef: DatabaseReference = database.getReference(FirebaseConfig.CONVERSATIONS_PATH)
    private val storageRepository = FirebaseStorageRepository(context)
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * Create a new conversation
     */
    suspend fun createConversation(title: String = "New Chat"): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            val conversationId = UUID.randomUUID().toString()
            
            val conversation = ChatConversation(
                id = conversationId,
                title = title,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                messageCount = 0
            )
            
            conversationsRef.child(userId).child(conversationId)
                .setValue(conversation.toMap()).await()
            
            Result.success(conversationId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all conversations for current user
     */
    suspend fun getConversations(limit: Int = 50): Result<List<ChatConversation>> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            
            val snapshot = conversationsRef.child(userId)
                .orderByChild("updatedAt")
                .limitToLast(limit)
                .get()
                .await()
            
            val conversations = mutableListOf<ChatConversation>()
            snapshot.children.forEach { convSnapshot ->
                try {
                    val conversation = convSnapshot.getValue(ChatConversation::class.java)
                    conversation?.let { conversations.add(it) }
                } catch (e: Exception) {
                    // Skip malformed conversations
                }
            }
            
            // Sort by updatedAt descending (most recent first)
            conversations.sortByDescending { it.updatedAt }
            Result.success(conversations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a message with optional file attachments to a specific conversation
     * @param conversationId The conversation ID
     * @param messageText The text message
     * @param fileUris List of file URIs to attach
     * @param onUploadProgress Progress callback for file uploads
     * @return Result with message ID
     */
    suspend fun sendMessage(
        conversationId: String,
        messageText: String,
        fileUris: List<Uri> = emptyList(),
        onUploadProgress: ((Int, Int) -> Unit)? = null
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            val messageId = UUID.randomUUID().toString()
            
            // Upload files if any
            val attachments = if (fileUris.isNotEmpty()) {
                val uploadResult = storageRepository.uploadMultipleFiles(fileUris, onUploadProgress)
                uploadResult.getOrNull() ?: emptyList()
            } else {
                emptyList()
            }
            
            // Create message object
            val message = ChatMessage(
                id = messageId,
                userId = userId,
                message = messageText,
                timestamp = System.currentTimeMillis(),
                attachments = attachments,
                isUser = true
            )
            
            // Save to Firebase under conversation
            messagesRef.child(userId).child(conversationId).child(messageId)
                .setValue(message.toMap()).await()
            
            // Update conversation metadata
            updateConversationMetadata(conversationId, messageText)
            
            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save an AI response to the conversation history
     */
    suspend fun saveAIMessage(conversationId: String, messageText: String): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            val messageId = UUID.randomUUID().toString()
            
            val message = ChatMessage(
                id = messageId,
                userId = "AI", // Identifier for AI sender
                message = messageText,
                timestamp = System.currentTimeMillis(),
                attachments = emptyList(),
                isUser = false
            )
            
            // Save to Firebase under the user's conversation path
            messagesRef.child(userId).child(conversationId).child(messageId)
                .setValue(message.toMap()).await()
            
            // Update conversation metadata (timestamp and last message)
            updateConversationMetadata(conversationId, messageText)
            
            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update conversation metadata (title, last updated, message count)
     */
    private suspend fun updateConversationMetadata(conversationId: String, firstMessage: String? = null) {
        try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            val convRef = conversationsRef.child(userId).child(conversationId)
            
            // Get current conversation
            val snapshot = convRef.get().await()
            val conversation = snapshot.getValue(ChatConversation::class.java)
            
            if (conversation != null) {
                val updates = hashMapOf<String, Any>(
                    "updatedAt" to System.currentTimeMillis(),
                    "messageCount" to (conversation.messageCount + 1)
                )
                
                // Auto-generate title from first message if title is "New Chat"
                if (conversation.title == "New Chat" && !firstMessage.isNullOrEmpty()) {
                    updates["title"] = generateConversationTitle(firstMessage)
                }
                
                convRef.updateChildren(updates).await()
            }
        } catch (e: Exception) {
            // Silently fail - metadata update is not critical
        }
    }
    
    /**
     * Generate conversation title from first message
     * Takes first 40 characters, truncates at word boundary
     */
    private fun generateConversationTitle(message: String): String {
        // Remove emojis and extra whitespace
        val cleaned = message.replace("[^\\p{L}\\p{N}\\p{P}\\p{Z}]".toRegex(), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
        
        if (cleaned.length <= 40) return cleaned
        
        // Truncate at word boundary
        val truncated = cleaned.substring(0, 40)
        val lastSpace = truncated.lastIndexOf(' ')
        
        return if (lastSpace > 20) {
            "${truncated.substring(0, lastSpace)}..."
        } else {
            "${truncated}..."
        }
    }
    
    /**
     * Get messages for a specific conversation
     */
    suspend fun getMessagesForConversation(
        conversationId: String,
        limit: Int = 100
    ): Result<List<ChatMessage>> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            
            val snapshot = messagesRef.child(userId).child(conversationId)
                .orderByChild("timestamp")
                .limitToLast(limit)
                .get()
                .await()
            
            val messages = mutableListOf<ChatMessage>()
            snapshot.children.forEach { messageSnapshot ->
                try {
                    val message = messageSnapshot.getValue(ChatMessage::class.java)
                    message?.let { messages.add(it) }
                } catch (e: Exception) {
                    // Skip malformed messages
                }
            }
            
            // Sort by timestamp
            messages.sortBy { it.timestamp }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send a message (backward compatible - creates default conversation)
     * @param messageText The text message
     * @param fileUris List of file URIs to attach
     * @param onUploadProgress Progress callback for file uploads
     * @return Result with message ID
     */
    suspend fun sendMessage(
        messageText: String,
        fileUris: List<Uri> = emptyList(),
        onUploadProgress: ((Int, Int) -> Unit)? = null
    ): Result<String> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            val messageId = UUID.randomUUID().toString()
            
            // Upload files if any
            val attachments = if (fileUris.isNotEmpty()) {
                val uploadResult = storageRepository.uploadMultipleFiles(fileUris, onUploadProgress)
                uploadResult.getOrNull() ?: emptyList()
            } else {
                emptyList()
            }
            
            // Create message object
            val message = ChatMessage(
                id = messageId,
                userId = userId,
                message = messageText,
                timestamp = System.currentTimeMillis(),
                attachments = attachments,
                isUser = true
            )
            
            // Save to Firebase
            messagesRef.child(userId).child(messageId).setValue(message.toMap()).await()
            
            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get messages for current user as a Flow
     */
    fun getMessagesFlow(): Flow<List<ChatMessage>> = callbackFlow {
        val userId = auth.currentUser?.uid ?: "anonymous"
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                
                snapshot.children.forEach { messageSnapshot ->
                    try {
                        val message = messageSnapshot.getValue(ChatMessage::class.java)
                        message?.let { messages.add(it) }
                    } catch (e: Exception) {
                        // Skip malformed messages
                    }
                }
                
                // Sort by timestamp
                messages.sortBy { it.timestamp }
                trySend(messages)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        val query = messagesRef.child(userId).orderByChild("timestamp")
        query.addValueEventListener(listener)
        
        awaitClose {
            query.removeEventListener(listener)
        }
    }
    
    /**
     * Get messages for current user (one-time fetch)
     */
    suspend fun getMessages(limit: Int = 50): Result<List<ChatMessage>> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            
            val snapshot = messagesRef.child(userId)
                .orderByChild("timestamp")
                .limitToLast(limit)
                .get()
                .await()
            
            val messages = mutableListOf<ChatMessage>()
            snapshot.children.forEach { messageSnapshot ->
                try {
                    val message = messageSnapshot.getValue(ChatMessage::class.java)
                    message?.let { messages.add(it) }
                } catch (e: Exception) {
                    // Skip malformed messages
                }
            }
            
            // Sort by timestamp
            messages.sortBy { it.timestamp }
            Result.success(messages)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            
            // Get message to delete attachments
            val snapshot = messagesRef.child(userId).child(messageId).get().await()
            val message = snapshot.getValue(ChatMessage::class.java)
            
            // Delete attachments from storage
            message?.attachments?.forEach { attachment ->
                storageRepository.deleteFile(attachment.url)
            }
            
            // Delete message from database
            messagesRef.child(userId).child(messageId).removeValue().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete a conversation and all its messages
     */
    suspend fun deleteConversation(conversationId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            
            // Delete all messages in the conversation
            val messagesSnapshot = messagesRef.child(userId).child(conversationId).get().await()
            messagesSnapshot.children.forEach { messageSnapshot ->
                val message = messageSnapshot.getValue(ChatMessage::class.java)
                // Delete attachments from storage
                message?.attachments?.forEach { attachment ->
                    storageRepository.deleteFile(attachment.url)
                }
            }
            
            // Delete all messages
            messagesRef.child(userId).child(conversationId).removeValue().await()
            
            // Delete conversation metadata
            conversationsRef.child(userId).child(conversationId).removeValue().await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Clear all messages for current user
     */
    suspend fun clearAllMessages(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: "anonymous"
            messagesRef.child(userId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
