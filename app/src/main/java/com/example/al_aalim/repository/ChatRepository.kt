package com.example.al_aalim.repository

import android.content.Context
import android.net.Uri
import com.example.al_aalim.config.FirebaseConfig
import com.example.al_aalim.models.Attachment
import com.example.al_aalim.models.ChatConversation
import com.example.al_aalim.models.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for Firebase Realtime Database operations
 */
class ChatRepository(private val context: Context) {
    
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
     * Returns the current authenticated user's UID, or null if not signed in.
     * All public functions MUST call this and return Result.failure() if null
     * to prevent reads/writes under a shared "anonymous" Firebase node.
     */
    private fun requireUserId(): String? = auth.currentUser?.uid
    
    /**
     * Create a new conversation
     */
    suspend fun createConversation(title: String = "New Chat"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = requireUserId()
                    ?: return@withContext Result.failure(Exception("Not authenticated"))
                val conversationId = UUID.randomUUID().toString()
                
                android.util.Log.d("ChatRepository", "Creating new conversation: $conversationId for user: $userId")

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
                android.util.Log.e("ChatRepository", "Error creating conversation", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get all conversations for current user
     */
    suspend fun getConversations(limit: Int = 50): Result<List<ChatConversation>> {
        return try {
            val userId = requireUserId()
                ?: return Result.failure(Exception("Not authenticated"))
            
            val snapshot = conversationsRef.child(userId)
                .orderByChild("updatedAt")
                .limitToLast(limit)
                .get()
                .await()
            
            val conversations = mutableListOf<ChatConversation>()
            snapshot.children.forEach { convSnapshot ->
                try {
                    val conversation = convSnapshot.getValue(ChatConversation::class.java)
                    conversation?.let { 
                        // Ensure ID is set even if it's missing from the database record
                        val idToUse = if (it.id.isEmpty()) convSnapshot.key ?: "" else it.id
                        conversations.add(it.copy(id = idToUse))
                    }
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
        return withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid ?: "anonymous"
                val messageId = UUID.randomUUID().toString()
                
                android.util.Log.d("ChatRepository", "Saving messageId: $messageId to conversationId: $conversationId")

                // Copy images to internal storage so they persist across sessions
                val attachments = fileUris.mapNotNull { uri ->
                    try {
                        val imagesDir = java.io.File(context.filesDir, "chat_images")
                        if (!imagesDir.exists()) imagesDir.mkdirs()
                        
                        val fileName = "${System.currentTimeMillis()}_${messageId.take(8)}.jpg"
                        val destFile = java.io.File(imagesDir, fileName)
                        
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        Attachment(
                            type = "image",
                            url = destFile.absolutePath,  // Permanent local path
                            fileName = fileName,
                            fileSize = destFile.length(),
                            mimeType = "image/jpeg",
                            uploadedAt = System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("ChatRepository", "Failed to copy image: ${e.message}")
                        null
                    }
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
                android.util.Log.e("ChatRepository", "Error sending message", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Save an AI response to the conversation history
     */
    suspend fun saveAIMessage(conversationId: String, messageText: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
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
                android.util.Log.e("ChatRepository", "Error saving AI message", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update conversation metadata (title, last updated, message count)
     */
    private suspend fun updateConversationMetadata(conversationId: String, firstMessage: String? = null) {
        try {
            val userId = requireUserId() ?: return
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
                    updates["title"] = com.example.al_aalim.utils.StringUtils.generateConversationTitle(firstMessage)
                }
                
                convRef.updateChildren(updates).await()
            }
        } catch (e: Exception) {
            // Silently fail - metadata update is not critical
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
            val userId = requireUserId()
                ?: return Result.failure(Exception("Not authenticated"))
            
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
     * Delete a message
     */
    suspend fun deleteMessage(conversationId: String, messageId: String): Result<Unit> {
        return try {
            val userId = requireUserId()
                ?: return Result.failure(Exception("Not authenticated"))
            
            // Get message to delete attachments
            val snapshot = messagesRef.child(userId).child(conversationId).child(messageId).get().await()
            val message = snapshot.getValue(ChatMessage::class.java)
            
            // Delete attachments from storage
            message?.attachments?.forEach { attachment ->
                storageRepository.deleteFile(attachment.url)
            }
            
            // Delete message from database
            messagesRef.child(userId).child(conversationId).child(messageId).removeValue().await()
            
            // Update the conversation metadata (optional, decrement count)
            val convRef = conversationsRef.child(userId).child(conversationId)
            val convSnapshot = convRef.get().await()
            val conv = convSnapshot.getValue(ChatConversation::class.java)
            if (conv != null && conv.messageCount > 0) {
                convRef.child("messageCount").setValue(conv.messageCount - 1).await()
            }
            
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
            val userId = requireUserId()
                ?: return Result.failure(Exception("Not authenticated"))
            
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
     * Rename a conversation
     */
    suspend fun updateConversationTitle(conversationId: String, newTitle: String): Result<String> {
        return try {
            val userId = requireUserId()
                ?: return Result.failure(Exception("Not authenticated"))
            android.util.Log.d("ChatRepository", "Renaming $conversationId to '$newTitle' for user $userId")
            
            val conversationRef = conversationsRef.child(userId).child(conversationId)
            
            withContext(Dispatchers.IO) {
                // Direct field update is sufficient for renaming
                conversationRef.updateChildren(mapOf(
                    "title" to newTitle,
                    "updatedAt" to System.currentTimeMillis()
                )).await()
            }
            
            val path = "conversations/$userId/$conversationId/title"
            android.util.Log.d("ChatRepository", "Rename operation completed for $conversationId at path: $path")
            Result.success(path)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "Rename failed", e)
            Result.failure(e)
        }
    }

    /**
     * Clear all messages for current user
     */
    suspend fun clearAllMessages(): Result<Unit> {
        return try {
            val userId = requireUserId()
                ?: return Result.failure(Exception("Not authenticated"))
            messagesRef.child(userId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Delete all conversations and their messages for current user
     */
    suspend fun deleteAllConversations(): Result<Unit> {
        val userId = requireUserId()
            ?: return Result.failure(Exception("Not authenticated"))
        android.util.Log.d("ChatRepository", "Starting deleteAllConversations for user: $userId")

        return try {
            // Get all conversations
            android.util.Log.d("ChatRepository", "Fetching conversations from: ${conversationsRef.child(userId).path}")
            val conversationsSnapshot = conversationsRef.child(userId).get().await()
            android.util.Log.d("ChatRepository", "Found ${conversationsSnapshot.childrenCount} conversations to delete")
            
            // Delete each conversation and its messages individually
            conversationsSnapshot.children.forEach { convSnapshot ->
                val conversationId = convSnapshot.key ?: return@forEach
                android.util.Log.d("ChatRepository", "Deleting conversation: $conversationId")
                
                try {
                    // 1. Delete attachments in this conversation
                    android.util.Log.d("ChatRepository", "Fetching messages for conversation: $conversationId to check for attachments")
                    val messagesSnapshot = messagesRef.child(userId).child(conversationId).get().await()
                    
                    messagesSnapshot.children.forEach { messageSnapshot ->
                        val message = messageSnapshot.getValue(ChatMessage::class.java)
                        message?.attachments?.forEach { attachment ->
                            android.util.Log.d("ChatRepository", "Deleting attachment: ${attachment.url}")
                            try {
                                storageRepository.deleteFile(attachment.url)
                            } catch (e: Exception) {
                                android.util.Log.e("ChatRepository", "Failed to delete attachment: ${attachment.url}", e)
                            }
                        }
                    }
                    
                    // 2. Delete all messages for this specific conversation
                    android.util.Log.d("ChatRepository", "Removing messages for conversation: $conversationId")
                    messagesRef.child(userId).child(conversationId).removeValue().await()
                    
                    // 3. Delete the conversation metadata itself
                    android.util.Log.d("ChatRepository", "Removing metadata for conversation: $conversationId")
                    conversationsRef.child(userId).child(conversationId).removeValue().await()
                    
                } catch (e: Exception) {
                    android.util.Log.e("ChatRepository", "Failed to delete conversation: $conversationId", e)
                    // Continue with next conversation even if one fails
                }
            }
            
            // Clean up old structure (direct messages under userId)
            try {
                android.util.Log.d("ChatRepository", "Checking for old-structure messages to clean up")
                val oldMessagesSnapshot = messagesRef.child(userId).get().await()
                oldMessagesSnapshot.children.forEach { msgSnapshot ->
                    if (msgSnapshot.hasChild("message")) {
                        android.util.Log.d("ChatRepository", "Deleting old-structure message: ${msgSnapshot.key}")
                        msgSnapshot.ref.removeValue().await()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatRepository", "Error during old-structure cleanup", e)
            }
            
            android.util.Log.d("ChatRepository", "deleteAllConversations completed")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ChatRepository", "CRITICAL ERROR in deleteAllConversations: ${e.message}", e)
            Result.failure(e)
        }
    }
}
