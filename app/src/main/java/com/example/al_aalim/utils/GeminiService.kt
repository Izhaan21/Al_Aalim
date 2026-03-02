package com.example.al_aalim.utils

import android.util.Log
import com.example.al_aalim.models.ChatMessage
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini AI Service for Al-Aalim Islamic Assistant
 * 
 * This service handles all AI chat interactions using Google's Gemini model.
 * Configure the API key before using.
 */
object GeminiService {
    
    private const val TAG = "GeminiService"
    
    // TODO: Replace with your actual Gemini API key
    // Get your API key from: https://makersuite.google.com/app/apikey
    private const val API_KEY = "AIzaSyBToArTla_mRmnmEO-yXCQ5D45D1dWcMjU"
    
    // System instruction for Islamic assistant context
    private const val SYSTEM_INSTRUCTION = """
        You are Al-Aalim, a knowledgeable, respectful, and concise Islamic AI assistant. 
        Your rules:
        1. Be Short & Crisp: Provide direct, to-the-point answers. Avoid long preambles.
        2. References First: Always include specific references from the Quran (Chapter:Verse) or Sahih Hadith to back up your points.
        3. Respectful Tone: Maintain a warm, welcoming "Al-Aalim" persona.
        4. Focus: Answer questions about Islam, Quran, Hadith, and Islamic practices.
        5. Language: Always respond in the same language the user uses.
        6. Limits: If unsure, acknowledge it and suggest consulting a qualified scholar.
        7. Arabic text from quraan as refrence in short
        
        Example: "Assalamu Alaikum. Prayer is mandatory. Quran 2:43 says: 'And establish prayer and give zakah...'"
    """

    private var generativeModel: GenerativeModel? = null
    private val chatHistory = mutableListOf<Content>()
    
    /**
     * Initialize the Gemini model
     */
    fun initialize(apiKey: String = API_KEY) {
        if (apiKey == "YOUR_GEMINI_API_KEY") {
            Log.e(TAG, "Please set your Gemini API key in GeminiService.kt")
            return
        }
        
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey,
            systemInstruction = content { text(SYSTEM_INSTRUCTION) }
        )
        
        Log.d(TAG, "Gemini model initialized")
    }
    
    /**
     * Send a message and get AI response
     * @param message User's message
     * @return AI response text or error message
     */
    suspend fun sendMessage(message: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val model = generativeModel
                if (model == null) {
                    // Try to initialize if not done
                    initialize()
                    if (generativeModel == null) {
                        return@withContext Result.failure(Exception("Gemini API key not configured"))
                    }
                }
                
                // Create chat with history for context
                val chat = generativeModel!!.startChat(history = chatHistory)
                
                // Send message and get response
                val response = chat.sendMessage(message)
                val responseText = response.text ?: "I couldn't generate a response. Please try again."
                
                // Add to history for context in future messages
                chatHistory.add(content("user") { text(message) })
                chatHistory.add(content("model") { text(responseText) })
                
                // Keep history limited to last 20 messages to avoid token limits
                if (chatHistory.size > 20) {
                    chatHistory.removeAt(0)
                    chatHistory.removeAt(0)
                }
                
                Log.d(TAG, "Response received: ${responseText.take(100)}...")
                Result.success(responseText)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error getting AI response: ${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Clear chat history (start a new conversation)
     */
    fun clearHistory() {
        chatHistory.clear()
        Log.d(TAG, "Chat history cleared")
    }
    
    /**
     * Set chat history from existing messages (for continuity when loading older chats)
     * @param messages List of ChatMessage from database
     */
    fun setChatHistory(messages: List<ChatMessage>) {
        chatHistory.clear()
        
        // Take only the last 20 messages for context efficiency
        val recentMessages = if (messages.size > 20) {
            messages.takeLast(20)
        } else {
            messages
        }
        
        recentMessages.forEach { msg ->
            if (msg.message.isNotEmpty()) {
                val role = if (msg.isUser) "user" else "model"
                chatHistory.add(content(role) { text(msg.message) })
            }
        }
        
        Log.d(TAG, "Chat history populated with ${chatHistory.size} messages")
    }
    
    /**
     * Check if service is configured
     */
    fun isConfigured(): Boolean {
        return API_KEY != "YOUR_GEMINI_API_KEY"
    }
}
