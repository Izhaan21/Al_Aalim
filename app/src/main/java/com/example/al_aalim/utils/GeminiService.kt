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
    private val API_KEY: String = BuildConfig.GEMINI_API_KEY

    private const val MODEL = "gemini-2.5-flash-lite"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    // -------------------------------------------------------------------------
    // System instruction
    // -------------------------------------------------------------------------
    private const val SYSTEM_INSTRUCTION = """
        You are Al-Aalim, a kind and knowledgeable Islamic AI assistant.

        STRICT RULES:

        1. KEEP IT SHORT: Maximum 3-5 sentences per response. Never write long paragraphs.
           Do NOT repeat the user's question. Get straight to the answer.

        2. CITE SOURCES: Mention a Quran verse or Hadith reference briefly (e.g., Quran 2:255).

        3. PERSONA: You are Al-Aalim. Be warm and humble.
           Say "Assalamualaikum" only at the very start of a brand-new conversation.

        4. LANGUAGE: Reply in the SAME language the user writes in.

        5. ARABIC: Include one short Arabic Quran quote only when directly relevant.

        6. NO FILLER: No mid-conversation greetings, no "Great question!", no unnecessary praise.
           Just answer directly.

        7. CONTEXT AWARENESS (most important rule for follow-ups):
           You are in an ongoing conversation. The conversation history is provided to you.
           - ALWAYS read the prior messages before answering.
           - If the user says "tell me more", "explain further", "what about that", "elaborate",
             "give an example", or uses pronouns like "it", "this", "that", "him", "her",
             they are referring to the PREVIOUS topic. Look at the last assistant reply and
             continue from there.
           - NEVER treat a follow-up as a brand-new standalone question when context exists.
           - If genuinely ambiguous, ask one short clarifying question.

        8. SCOPE — Islamic topics only:
           You ONLY answer questions related to Islam, Quran, Hadith, Islamic history,
           Islamic jurisprudence (fiqh), Arabic as it relates to Islam, Muslim scholars,
           and Islamic lifestyle.
           If the user asks about ANYTHING unrelated to Islam (coding, sports, entertainment,
           general science, cooking, etc.) reply ONLY with:
           "I'm Al-Aalim, your Islamic assistant. I can only help with Islamic topics.
           Please ask me about Quran, Hadith, prayer, Islamic history, or any other
           Islamic subject. 🤲"

        9. IMAGE ANALYSIS — when the user sends an image:
           - Islamic-related image (mosque, Quran page, Arabic calligraphy, Ka'bah, prayer,
             Islamic art): provide a warm, knowledgeable response with Quran/Hadith references.
           - Non-Islamic image (selfie, food, nature, technology, random objects): reply ONLY with:
             "I can only help with Islamic-related images. Please share an image related to Islam,
             such as Quranic text, a mosque, or Islamic art, and I'll be happy to assist! 🤲"
           - Vulgar, inappropriate, or haram content: reply ONLY with:
             "Astaghfirullah. This image contains inappropriate content. As an Islamic assistant,
             I cannot engage with such material. Please share something appropriate. 🤲"
    """

    // -------------------------------------------------------------------------
    // Chat history
    // -------------------------------------------------------------------------
    private data class HistoryEntry(val role: String, val text: String)
    private val chatHistory = mutableListOf<HistoryEntry>()

    private var initialized = false

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------
    fun initialize(apiKey: String = API_KEY) {
        if (apiKey.isEmpty()) {
            Log.e(TAG, "Gemini API key is not configured. Add GEMINI_API_KEY to local.properties.")
            return
        }
        initialized = true
        Log.d(TAG, "GeminiService initialized (REST API, model=$MODEL)")
    }

    // -------------------------------------------------------------------------
    // Public send helpers
    // -------------------------------------------------------------------------
    suspend fun sendMessage(message: String): Result<String> =
        sendMessage(message, emptyList())

    /**
     * Send a message with optional images and get an AI response.
     * @param message     The text message (may be empty if only images are attached).
     * @param imageDataList List of Pair(base64Data, mimeType) for each image.
     */
    suspend fun sendMessage(
        message: String,
        imageDataList: List<Pair<String, String>>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
                if (!initialized) {
                    return@withContext Result.failure(Exception("Gemini API key not configured"))
                }
            }

            val requestBody = buildRequestBody(message, imageDataList)

            val url = URL("$BASE_URL/$MODEL:generateContent?key=$API_KEY")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
                connectTimeout = 15_000
                readTimeout  = 30_000
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = BufferedReader(
                    InputStreamReader(connection.inputStream, "UTF-8")
                ).use { it.readText() }

                val aiText = parseResponse(responseText)
                if (aiText != null) {
                    // Store text in history (images are not persisted – only the text prompt).
                    val historyText = when {
                        message.isNotEmpty() -> message
                        imageDataList.isNotEmpty() -> "[Image sent]"
                        else -> ""
                    }
                    if (historyText.isNotEmpty()) {
                        chatHistory.add(HistoryEntry("user",  historyText))
                        chatHistory.add(HistoryEntry("model", aiText))
                    }

                    // Keep only the last 10 entries (5 exchanges) for richer context.
                    if (chatHistory.size > 10) {
                        val trimmed = chatHistory.takeLast(10)
                        chatHistory.clear()
                        chatHistory.addAll(trimmed)
                    }

                    Log.d(TAG, "Response ok (history=${chatHistory.size}): ${aiText.take(80)}…")
                    Result.success(aiText)
                } else {
                    Result.failure(Exception("Empty response from AI. Please try again."))
                }
            } else {
                val errorText = BufferedReader(
                    InputStreamReader(connection.errorStream ?: connection.inputStream, "UTF-8")
                ).use { it.readText() }
                Log.e(TAG, "API error $responseCode: $errorText")

                val friendlyMsg = when (responseCode) {
                    429  -> "AI quota exceeded. Please wait a moment and try again."
                    400  -> "Invalid request. Please start a new chat."
                    403  -> "API key invalid or expired. Please update your Gemini API key."
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

    // -------------------------------------------------------------------------
    // Request builder
    // -------------------------------------------------------------------------
    private fun buildRequestBody(
        userMessage: String,
        imageDataList: List<Pair<String, String>> = emptyList()
    ): JSONObject {
        val root = JSONObject()

        // System instruction
        root.put(
            "system_instruction",
            JSONObject().put(
                "parts",
                JSONArray().put(JSONObject().put("text", SYSTEM_INSTRUCTION.trimIndent()))
            )
        )

        val contents = JSONArray()

        // --- Priming exchange (first message only) ----------------------------
        // Kept minimal so it does not pollute the context window or confuse
        // the model about what "previous" conversation existed.
        if (chatHistory.isEmpty()) {
            contents.put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(
                        JSONObject().put(
                            "text",
                            "Introduce yourself very briefly in one sentence and tell me you are ready to help with Islamic questions."
                        )
                    ))
            )
            contents.put(
                JSONObject()
                    .put("role", "model")
                    .put("parts", JSONArray().put(
                        JSONObject().put(
                            "text",
                            "As-salamu alaykum! I am Al-Aalim, your Islamic guide — ask me anything about Quran, Hadith, or Islamic life. 🤲"
                        )
                    ))
            )
        }

        // --- Full conversation history -----------------------------------------
        // Sending the real history is what gives the model context for follow-ups.
        for (entry in chatHistory) {
            contents.put(
                JSONObject()
                    .put("role", entry.role)
                    .put("parts", JSONArray().put(JSONObject().put("text", entry.text)))
            )
        }

        // --- Current user turn ------------------------------------------------
        val userParts = JSONArray()

        // Images first
        for ((base64Data, mimeType) in imageDataList) {
            userParts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", mimeType).put("data", base64Data)
                )
            )
        }

        // Text (or a fallback prompt for image-only messages)
        val textContent = when {
            userMessage.isNotEmpty()       -> userMessage
            imageDataList.isNotEmpty()     -> "Analyze this image. Follow your image analysis rules strictly."
            else                           -> ""
        }
        if (textContent.isNotEmpty()) {
            userParts.put(JSONObject().put("text", textContent))
        }

        contents.put(
            JSONObject().put("role", "user").put("parts", userParts)
        )

        root.put("contents", contents)

        // Generation config
        root.put(
            "generationConfig",
            JSONObject()
                .put("temperature", 0.5)
                .put("maxOutputTokens", 512)
        )

        return root
    }

    // -------------------------------------------------------------------------
    // Response parser
    // -------------------------------------------------------------------------
    private fun parseResponse(json: String): String? {
        return try {
            val root = JSONObject(json)
            val candidates = root.optJSONArray("candidates")

            if (candidates == null || candidates.length() == 0) {
                null
            } else {
                val parts = candidates.getJSONObject(0)
                    .optJSONObject("content")
                    ?.optJSONArray("parts")

                if (parts == null) {
                    null
                } else {
                    val sb = StringBuilder()
                    for (i in 0 until parts.length()) {
                        sb.append(parts.getJSONObject(i).optString("text", ""))
                    }
                    sb.toString().trim().ifEmpty { null }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            null
        }
    }

    // -------------------------------------------------------------------------
    // History management
    // -------------------------------------------------------------------------
    fun clearHistory() {
        chatHistory.clear()
        Log.d(TAG, "Chat history cleared")
    }

    /**
     * Restore history from persisted messages (e.g. when reopening an old chat).
     * Only the last 10 messages are loaded to stay within context budget.
     */
    fun setChatHistory(messages: List<ChatMessage>) {
        chatHistory.clear()
        val recent = if (messages.size > 10) messages.takeLast(10) else messages
        recent.forEach { msg ->
            if (msg.message.isNotEmpty()) {
                chatHistory.add(HistoryEntry(if (msg.isUser) "user" else "model", msg.message))
            }
        }
        Log.d(TAG, "Chat history loaded: ${chatHistory.size} entries")
    }

    fun isConfigured(): Boolean = API_KEY.isNotEmpty()
}