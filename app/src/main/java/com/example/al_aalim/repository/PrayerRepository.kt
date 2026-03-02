package com.example.al_aalim.repository

import android.content.Context
import android.util.Log
import com.example.al_aalim.model.PrayerName
import com.example.al_aalim.model.PrayerRecord
import com.example.al_aalim.model.PrayerStats
import com.example.al_aalim.model.PrayerTime
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Repository for managing prayer tracking data in Firebase
 */
class PrayerRepository(private val context: Context) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "PrayerRepository"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * Get current user ID
     */
    private fun getUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Mark a prayer as completed for today
     */
    suspend fun markPrayerCompleted(prayerName: String): Result<Unit> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            val today = dateFormat.format(Date())
            
            // Get or create today's prayer record
            val recordRef = firestore.collection("prayerRecords")
                .document(userId)
                .collection("records")
                .document(today)
            
            val existingRecord = recordRef.get().await()
            val prayers = existingRecord.toObject(PrayerRecord::class.java)?.prayers?.toMutableMap() 
                ?: mutableMapOf()
            
            // Update the specific prayer
            prayers[prayerName] = PrayerTime(
                time = prayers[prayerName]?.time ?: "",
                isCompleted = true,
                completedAt = Timestamp.now()
            )
            
            // Count completed prayers
            val completionCount = prayers.values.count { it.isCompleted }
            
            // Update the record
            val updatedRecord = PrayerRecord(
                userId = userId,
                date = today,
                prayers = prayers,
                completionCount = completionCount,
                streakContinued = completionCount == 5
            )
            
            recordRef.set(updatedRecord).await()
            
            // Update stats
            updatePrayerStats(userId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking prayer as completed", e)
            Result.failure(e)
        }
    }
    
    /**
     * Unmark a prayer (if user made mistake)
     */
    suspend fun unmarkPrayer(prayerName: String): Result<Unit> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            val today = dateFormat.format(Date())
            
            val recordRef = firestore.collection("prayerRecords")
                .document(userId)
                .collection("records")
                .document(today)
            
            val existingRecord = recordRef.get().await()
            val prayers = existingRecord.toObject(PrayerRecord::class.java)?.prayers?.toMutableMap() 
                ?: mutableMapOf()
            
            // Update the specific prayer
            prayers[prayerName] = PrayerTime(
                time = prayers[prayerName]?.time ?: "",
                isCompleted = false,
                completedAt = null
            )
            
            // Count completed prayers
            val completionCount = prayers.values.count { it.isCompleted }
            
            // Update the record
            val updatedRecord = PrayerRecord(
                userId = userId,
                date = today,
                prayers = prayers,
                completionCount = completionCount,
                streakContinued = completionCount == 5
            )
            
            recordRef.set(updatedRecord).await()
            
            // Update stats
            updatePrayerStats(userId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unmarking prayer", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get today's prayer record
     */
    suspend fun getTodaysPrayerRecord(latitude: Double, longitude: Double): Result<PrayerRecord> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            val today = dateFormat.format(Date())
            
            val recordRef = firestore.collection("prayerRecords")
                .document(userId)
                .collection("records")
                .document(today)
            
            val document = recordRef.get().await()
            
            if (document.exists()) {
                val record = document.toObject(PrayerRecord::class.java)
                Result.success(record ?: createEmptyRecord(userId, today, latitude, longitude))
            } else {
                // Create new record with calculated prayer times
                val newRecord = createEmptyRecord(userId, today, latitude, longitude)
                recordRef.set(newRecord).await()
                Result.success(newRecord)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's prayer record", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create empty prayer record with calculated times
     */
    private fun createEmptyRecord(userId: String, date: String, latitude: Double, longitude: Double): PrayerRecord {
        val prayerTimes = calculatePrayerTimes(latitude, longitude)
        
        val prayers = mutableMapOf<String, PrayerTime>()
        PrayerName.getAllPrayers().forEach { prayer ->
            prayers[prayer.name] = PrayerTime(
                time = prayerTimes[prayer.name] ?: "00:00",
                isCompleted = false,
                completedAt = null
            )
        }
        
        return PrayerRecord(
            userId = userId,
            date = date,
            prayers = prayers,
            completionCount = 0,
            streakContinued = false
        )
    }
    
    /**
     * Calculate prayer times using astronomical calculations
     * Based on the method used by the Muslim World League
     */
    private fun calculatePrayerTimes(latitude: Double, longitude: Double): Map<String, String> {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val timeZone = calendar.timeZone.rawOffset / (1000.0 * 60.0 * 60.0)
        
        // Julian date
        val julianDate = calculateJulianDate(year, month, day)
        
        // Calculate prayer times
        val times = mutableMapOf<String, String>()
        
        // Using simplified calculations for demo
        // In production, you'd use a library like PrayTimes or similar
        val sunriseTime = calculateSunrise(julianDate, latitude, longitude, timeZone)
        val sunsetTime = calculateSunset(julianDate, latitude, longitude, timeZone)
        val noonTime = calculateNoon(julianDate, longitude, timeZone)
        
        // Fajr (before sunrise)
        val fajr = addMinutesToTime(sunriseTime, -100)
        times[PrayerName.FAJR.name] = formatTime(fajr)
        
        // Dhuhr (after noon)
        times[PrayerName.DHUHR.name] = formatTime(noonTime + 0.05) // 5 mins after noon
        
        // Asr (afternoon)
        val asr = noonTime + 3.5 // Roughly 3.5 hours after noon
        times[PrayerName.ASR.name] = formatTime(asr)
        
        // Maghrib (sunset)
        times[PrayerName.MAGHRIB.name] = formatTime(sunsetTime + 0.05)
        
        // Isha (after sunset)
        val isha = addMinutesToTime(sunsetTime, 90)
        times[PrayerName.ISHA.name] = formatTime(isha)
        
        return times
    }
    
    private fun calculateJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = Math.floor(y / 100.0)
        val b = 2 - a + Math.floor(a / 4.0)
        
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + day + b - 1524.5
    }
    
    private fun calculateSunrise(jd: Double, lat: Double, lng: Double, tz: Double): Double {
        // Simplified sunrise calculation
        val noon = calculateNoon(jd, lng, tz)
        return noon - 6.0 // Approximate
    }
    
    private fun calculateSunset(jd: Double, lat: Double, lng: Double, tz: Double): Double {
        // Simplified sunset calculation
        val noon = calculateNoon(jd, lng, tz)
        return noon + 6.0 // Approximate
    }
    
    private fun calculateNoon(jd: Double, lng: Double, tz: Double): Double {
        val t = (jd - 2451545.0) / 36525.0
        val meanLongitude = 280.46646 + 36000.76983 * t + 0.0003032 * t * t
        val equation = -lng / 15.0 + tz
        return 12.0 + equation / 60.0
    }
    
    private fun addMinutesToTime(time: Double, minutes: Int): Double {
        return time + (minutes / 60.0)
    }
    
    private fun formatTime(decimalTime: Double): String {
        var time = decimalTime
        time = time - Math.floor(time / 24.0) * 24.0 // Ensure within 24 hours
        
        val hours = Math.floor(time).toInt()
        val minutes = ((time - hours) * 60).toInt()
        
        return String.format("%02d:%02d", hours, minutes)
    }
    
    /**
     * Update prayer statistics for user
     */
    private suspend fun updatePrayerStats(userId: String) {
        try {
            val statsRef = firestore.collection("prayerStats").document(userId)
            
            // Get last 30 days of records
            val thirtyDaysAgo = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -30)
            }
            val thirtyDaysAgoStr = dateFormat.format(thirtyDaysAgo.time)
            
            val recentRecords = firestore.collection("prayerRecords")
                .document(userId)
                .collection("records")
                .whereGreaterThanOrEqualTo("date", thirtyDaysAgoStr)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val records = recentRecords.toObjects(PrayerRecord::class.java)
            
            // Calculate current streak
            var currentStreak = 0
            for (record in records.sortedByDescending { it.date }) {
                if (record.completionCount == 5) {
                    currentStreak++
                } else {
                    break
                }
            }
            
            // Calculate longest streak (would need more historical data)
            val longestStreak = maxOf(currentStreak, currentStreak) // Simplified
            
            // Calculate total prayers completed
            val totalPrayers = records.sumOf { it.completionCount }
            
            // Calculate weekly completion rate (last 7 days)
            val weekRecords = records.take(7)
            val weeklyTotal = weekRecords.size * 5 // 5 prayers per day
            val weeklyCompleted = weekRecords.sumOf { it.completionCount }
            val weeklyRate = if (weeklyTotal > 0) (weeklyCompleted * 100f / weeklyTotal) else 0f
            
            // Calculate monthly completion rate
            val monthlyTotal = records.size * 5
            val monthlyCompleted = totalPrayers
            val monthlyRate = if (monthlyTotal > 0) (monthlyCompleted * 100f / monthlyTotal) else 0f
            
            val stats = PrayerStats(
                userId = userId,
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                totalPrayersCompleted = totalPrayers,
                weeklyCompletionRate = weeklyRate,
                monthlyCompletionRate = monthlyRate,
                lastUpdated = Timestamp.now()
            )
            
            statsRef.set(stats).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating prayer stats", e)
        }
    }
    
    /**
     * Get prayer statistics for current user
     */
    suspend fun getPrayerStats(): Result<PrayerStats> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            
            val statsRef = firestore.collection("prayerStats").document(userId)
            val document = statsRef.get().await()
            
            if (document.exists()) {
                val stats = document.toObject(PrayerStats::class.java)
                Result.success(stats ?: PrayerStats(userId = userId))
            } else {
                // Create empty stats
                val emptyStats = PrayerStats(userId = userId)
                statsRef.set(emptyStats).await()
                Result.success(emptyStats)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting prayer stats", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get prayer records for a date range
     */
    suspend fun getPrayerRecords(startDate: String, endDate: String): Result<List<PrayerRecord>> {
        return try {
            val userId = getUserId() ?: return Result.failure(Exception("User not logged in"))
            
            val records = firestore.collection("prayerRecords")
                .document(userId)
                .collection("records")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .await()
            
            Result.success(records.toObjects(PrayerRecord::class.java))
        } catch (e: Exception) {
            Log.e(TAG, "Error getting prayer records", e)
            Result.failure(e)
        }
    }
}
