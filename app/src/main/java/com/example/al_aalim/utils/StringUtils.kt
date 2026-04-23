package com.example.al_aalim.utils

object StringUtils {
    /**
     * Generates a safe and truncated conversation title from a raw message string.
     */
    fun generateConversationTitle(message: String): String {
        val cleaned = message
            .replace(Regex("""[^\p{L}\p{N}\p{P}\p{Z}]"""), "")  // strip emojis / non-printable chars
            .replace(Regex("""\s+"""), " ")                         // collapse whitespace
            .trim()

        if (cleaned.length <= 40) return cleaned

        val truncated = cleaned.substring(0, 40)
        val lastSpace = truncated.lastIndexOf(' ')

        return if (lastSpace > 20) "${truncated.substring(0, lastSpace)}..." else "${truncated}..."
    }
}
