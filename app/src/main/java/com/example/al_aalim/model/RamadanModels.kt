package com.example.al_aalim.model

import java.util.*

/**
 * Represents a single day in Ramadan
 */
data class RamadanDay(
    val dayNumber: Int,              // 1-30
    val date: Date,                  // Gregorian date
    val hijriDate: String,           // e.g., "1 Ramadan 1447"
    val suhoorTime: String,          // HH:mm format (Fajr time)
    val iftarTime: String,           // HH:mm format (Maghrib time)
    val isToday: Boolean = false     // Highlight current day
)

/**
 * Represents the full Ramadan month
 */
data class RamadanMonth(
    val year: Int,                   // Gregorian year (e.g., 2026)
    val hijriYear: Int,              // Hijri year (e.g., 1447)
    val startDate: Date,             // First day of Ramadan
    val endDate: Date,               // Last day of Ramadan
    val days: List<RamadanDay>,      // All 30 days
    val location: String = ""        // Location name for display
)

/**
 * User preferences for Ramadan calendar
 */
data class RamadanPreferences(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationName: String = "",
    val lastUpdated: Long = 0L       // Timestamp of last update
)

/**
 * Countdown information for next Suhoor/Iftar
 */
data class RamadanCountdown(
    val type: CountdownType,         // SUHOOR or IFTAR
    val timeRemaining: String,       // e.g., "2h 30m"
    val eventTime: String,           // HH:mm format
    val isActive: Boolean = true     // Whether Ramadan is currently active
)

enum class CountdownType {
    SUHOOR,
    IFTAR
}
