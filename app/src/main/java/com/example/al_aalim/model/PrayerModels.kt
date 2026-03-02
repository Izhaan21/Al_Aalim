package com.example.al_aalim.model

import com.google.firebase.Timestamp

/**
 * Data class representing a prayer
 */
data class Prayer(
    val name: String,           // Fajr, Dhuhr, Asr, Maghrib, Isha
    val time: String,           // HH:mm format
    val isCompleted: Boolean = false
) 

/**
 * Data class representing daily prayer record
 */
data class PrayerRecord(
    val userId: String = "",
    val date: String = "",              // Format: YYYY-MM-DD
    val prayers: Map<String, PrayerTime> = emptyMap(),
    val completionCount: Int = 0,       // Number of prayers completed (0-5)
    val streakContinued: Boolean = false // Whether this day continued the streak
)

/**
 * Individual prayer time and completion status
 */
data class PrayerTime(
    val time: String = "",              // HH:mm format
    val isCompleted: Boolean = false,
    val completedAt: Timestamp? = null  // When the prayer was marked as completed
)

/**
 * Prayer statistics for a user
 */
data class PrayerStats(
    val userId: String = "",
    val currentStreak: Int = 0,         // Current consecutive days with all 5 prayers
    val longestStreak: Int = 0,         // Longest ever streak
    val totalPrayersCompleted: Int = 0, // All-time total
    val weeklyCompletionRate: Float = 0f, // Percentage (0-100)
    val monthlyCompletionRate: Float = 0f, // Percentage (0-100)
    val lastUpdated: Timestamp? = null  // Changed to nullable to avoid Timestamp.now() in default
)

/**
 * Prayer names enum
 */
enum class PrayerName(val displayName: String) {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha");
    
    companion object {
        fun getAllPrayers() = values().toList()
    }
}
