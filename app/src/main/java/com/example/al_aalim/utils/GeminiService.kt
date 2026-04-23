package com.example.al_aalim.utils

import android.util.Log
import com.example.al_aalim.BuildConfig
import com.example.al_aalim.models.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini AI Service for Al-Aalim Islamic Assistant
 * 
 * Uses the FREE Gemini REST API directly (no SDK dependency).
 * Model: gemini-2.5-flash-lite (free tier, generous limits).
 * Endpoint: https://generativelanguage.googleapis.com/v1beta/
 */
object GeminiService {
    
    private const val TAG = "GeminiService"
    
    // Gemini API key – injected at build time from local.properties via BuildConfig.
    // Never hardcode secrets in source files.
    private val API_KEY: String = BuildConfig.GEMINI_API_KEY
    
    // Free-tier model – gemini-2.5-flash-lite for fastest responses
    private const val MODEL = "gemini-2.5-flash-lite"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    
    // System instruction for Islamic assistant context
    private const val SYSTEM_INSTRUCTION = """
        You are Al-Aalim, a kind and knowledgeable Islamic AI assistant.
        
        STRICT RULES:
        1. KEEP IT SHORT: Maximum 3-5 sentences per response. Never write long paragraphs. Do NOT repeat the user's question back. Get straight to the answer.
        2. CITE SOURCES: Mention Quran verse or Hadith reference briefly (e.g., Quran 2:255).
        3. PERSONA: You are Al-Aalim. Be warm and humble. Say "As-salamu alaykum" only at the very start of a new conversation.
        4. LANGUAGE: Reply in the SAME language the user writes in.
        5. ARABIC: Include one short Arabic Quran quote only when directly relevant.
        6. NO FILLER: No greetings mid-conversation, no "Great question!", no unnecessary praise. Just answer directly.
        7. NO REPEATING: Each answer must be FRESH and INDEPENDENT. NEVER repeat, summarize, or reference your previous answers. Treat each question as standalone. If the user asks a new question, answer ONLY that question — do not include information from your earlier replies.
        8. IMAGE ANALYSIS: When the user sends an image:
           - If the image is Islamic-related (mosque, Quran, Arabic calligraphy, Islamic art, prayer, Ka'bah, etc.), provide a warm, knowledgeable response about what you see with relevant Quran/Hadith references.
           - If the image is NOT related to Islam (random photos, selfies, food, nature, etc.), reply with: "I can only help with Islamic-related images. Please share an image related to Islam, such as Quranic text, a mosque, or Islamic art, and I'll be happy to assist! 🤲"
           - If the image contains vulgar, inappropriate, or haram content, reply ONLY with: "Astaghfirullah. This image contains inappropriate content. As an Islamic assistant, I cannot engage with such material. Please share something appropriate. 🤲"
    """

    // Chat history as simple list of role+text pairs
    private data class HistoryEntry(val role: String, val text: String)
    private val chatHistory = mutableListOf<HistoryEntry>()
    
    private var initialized = false
    
    /**
     * Initialize the service (lightweight – just validates the key)
     */
    fun initialize(apiKey: String = API_KEY) {
        if (apiKey.isEmpty()) {
            Log.e(TAG, "Gemini API key is not configured. Add GEMINI_API_KEY to local.properties.")
            return
        }
        initialized = true
        Log.d(TAG, "GeminiService initialized (REST API, model=$MODEL)")
    }
    
    /**
     * Send a text-only message and get AI response
     */
    suspend fun sendMessage(message: String): Result<String> {
        return sendMessage(message, emptyList())
    }

    /**
     * Send a message with optional images and get AI response via the free Gemini REST API
     * @param message The text message (can be empty if only images)
     * @param imageDataList List of Pair(base64Data, mimeType) for each image
     */
    suspend fun sendMessage(message: String, imageDataList: List<Pair<String, String>>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!initialized) {
                    initialize()
                    if (!initialized) {
                        return@withContext Result.failure(Exception("Gemini API key not configured"))
                    }
                }

                // Build the request JSON
                val requestBody = buildRequestBody(message, imageDataList)
                
                // Make the HTTP call
                val url = URL("$BASE_URL/$MODEL:generateContent?key=$API_KEY")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                
                // Write body
                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseText = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { it.readText() }
                    val aiText = parseResponse(responseText)
                    
                    if (aiText != null) {
                        // Add to history (text only, images aren't stored in history)
                        val historyText = if (message.isNotEmpty()) message else "[Image sent]"
                        chatHistory.add(HistoryEntry("user", historyText))
                        chatHistory.add(HistoryEntry("model", aiText))
                        
                        // Fix 7: Trim history to last 3 exchanges (6 entries).
                        // Original double removeAt(0) was buggy — after the first removal the list
                        // re-indexes, so the second call removes the wrong item.
                        // takeLast is index-safe and handles any excess in one step.
                        if (chatHistory.size > 6) {
                            val trimmed = chatHistory.takeLast(6)
                            chatHistory.clear()
                            chatHistory.addAll(trimmed)
                        }
                        
                        Log.d(TAG, "Response received (history=${chatHistory.size}): ${aiText.take(80)}...")
                        Result.success(aiText)
                    } else {
                        Result.failure(Exception("Empty response from AI. Please try again."))
                    }
                } else {
                    val errorText = BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream, "UTF-8")).use { it.readText() }
                    Log.e(TAG, "API error $responseCode: $errorText")
                    
                    val friendlyMsg = when (responseCode) {
                        429 -> "AI quota exceeded. Please wait a moment and try again."
                        400 -> "Invalid request. Please start a new chat."
                        403 -> "API key invalid or expired. Please update your Gemini API key."
                        else -> "Server error ($responseCode). Please try again."
                    }
                    Result.failure(Exception(friendlyMsg))
                }
                
            } catch (e: java.net.SocketTimeoutException) {
                Log.e(TAG, "Timeout: ${e.message}", e)
                Result.failure(Exception("Request timed out. Check your internet connection."))
            } catch (e: java.net.UnknownHostException) {
                Log.e(TAG, "No internet: ${e.message}", e)
                Result.failure(Exception("No internet connection. Please check your network."))
            } catch (e: Exception) {
                Log.e(TAG, "Error: ${e.message}", e)
                Result.failure(Exception("Failed to get AI response: ${e.message}"))
            }
        }
    }
    
    /**
     * Build the JSON request body with system instruction + chat history + current message (text + optional images)
     */
    private fun buildRequestBody(userMessage: String, imageDataList: List<Pair<String, String>> = emptyList()): JSONObject {
        val root = JSONObject()
        
        // System instruction
        val systemInstruction = JSONObject()
        val systemParts = JSONArray()
        systemParts.put(JSONObject().put("text", SYSTEM_INSTRUCTION.trimIndent()))
        systemInstruction.put("parts", systemParts)
        root.put("system_instruction", systemInstruction)
        
        // Contents = history + current user message
        val contents = JSONArray()
        
        // Inject a hidden priming exchange for new conversations to enforce persona
        if (chatHistory.isEmpty()) {
            val primingUser = JSONObject()
            primingUser.put("role", "user")
            val primingUserParts = JSONArray()
            primingUserParts.put(JSONObject().put("text", "Introduce yourself briefly. Remember: you must strictly follow all your rules — cite Quran/Hadith, match my language, stay concise, and stay in character as Al-Aalim."))
            primingUser.put("parts", primingUserParts)
            contents.put(primingUser)
            
            val primingModel = JSONObject()
            primingModel.put("role", "model")
            val primingModelParts = JSONArray()
            primingModelParts.put(JSONObject().put("text", "As-salamu alaykum! I am Al-Aalim, your Islamic guide. Ask me anything — I'll keep it brief with Quran & Hadith references. 🤲"))
            primingModel.put("parts", primingModelParts)
            contents.put(primingModel)
        }
        
        // Add chat history
        for (entry in chatHistory) {
            val contentObj = JSONObject()
            contentObj.put("role", entry.role)
            val parts = JSONArray()
            parts.put(JSONObject().put("text", entry.text))
            contentObj.put("parts", parts)
            contents.put(contentObj)
        }
        
        // Add current user message with optional images
        val userContent = JSONObject()
        userContent.put("role", "user")
        val userParts = JSONArray()

        // Add images first (as inline_data)
        for ((base64Data, mimeType) in imageDataList) {
            val inlineData = JSONObject()
            inlineData.put("mime_type", mimeType)
            inlineData.put("data", base64Data)
            userParts.put(JSONObject().put("inline_data", inlineData))
        }

        // Add text (or a prompt for image-only messages)
        val textContent = if (userMessage.isNotEmpty()) {
            userMessage
        } else if (imageDataList.isNotEmpty()) {
            "Analyze this image. Follow your image analysis rules strictly."
        } else {
            ""
        }
        if (textContent.isNotEmpty()) {
            userParts.put(JSONObject().put("text", textContent))
        }

        userContent.put("parts", userParts)
        contents.put(userContent)
        
        root.put("contents", contents)
        
        // Generation config – optimized for fast, short responses
        val genConfig = JSONObject()
        genConfig.put("temperature", 0.5)
        genConfig.put("maxOutputTokens", 512)
        root.put("generationConfig", genConfig)
        
        return root
    }
    
    /**
     * Parse the JSON response to extract the text
     */
    private fun parseResponse(json: String): String? {
        return try {
            val root = JSONObject(json)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            
            val sb = StringBuilder()
            for (i in 0 until parts.length()) {
                val text = parts.getJSONObject(i).optString("text", "")
                sb.append(text)
            }
            
            val result = sb.toString().trim()
            if (result.isEmpty()) null else result
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            null
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
     */
    fun setChatHistory(messages: List<ChatMessage>) {
        chatHistory.clear()
        
        // Take only the last 6 messages (3 exchanges) for context
        val recentMessages = if (messages.size > 6) {
            messages.takeLast(6)
        } else {
            messages
        }
        
        recentMessages.forEach { msg ->
            if (msg.message.isNotEmpty()) {
                val role = if (msg.isUser) "user" else "model"
                chatHistory.add(HistoryEntry(role, msg.message))
            }
        }
        
        Log.d(TAG, "Chat history populated with ${chatHistory.size} entries")
    }
    
    /**
     * Check if service is configured
     */
    fun isConfigured(): Boolean = API_KEY.isNotEmpty()
}
