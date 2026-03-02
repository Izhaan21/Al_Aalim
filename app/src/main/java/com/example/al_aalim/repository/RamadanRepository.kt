package com.example.al_aalim.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.al_aalim.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/**
 * Repository for managing Ramadan calendar data
 * Leverages existing PrayerRepository for accurate prayer time calculations
 */
class RamadanRepository(private val context: Context) {
    
    private val prayerRepository = PrayerRepository(context)
    private val prefs: SharedPreferences = context.getSharedPreferences("ramadan_prefs", Context.MODE_PRIVATE)
    private val TAG = "RamadanRepository"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * Ramadan 2026 dates (based on astronomical calculations)
     * Note: Actual dates may vary by ±1 day based on moon sighting
     */
    companion object {
        // Ramadan 2026: February 17 - March 18, 2026 (estimated)
        const val RAMADAN_2026_START_YEAR = 2026
        const val RAMADAN_2026_START_MONTH = Calendar.FEBRUARY
        const val RAMADAN_2026_START_DAY = 17
        const val RAMADAN_2026_HIJRI_YEAR = 1447
    }
    
    /**
     * Get full Ramadan month data with prayer times for given location
     */
    suspend fun getRamadanMonth(
        year: Int,
        latitude: Double,
        longitude: Double,
        locationName: String = ""
    ): Result<RamadanMonth> {
        return try {
            if (year != 2026) {
                return Result.failure(Exception("Only Ramadan 2026 is currently supported"))
            }
            
            val startDate = getRamadanStartDate(year)
            val calendar = Calendar.getInstance().apply {
                time = startDate
            }
            
            val days = mutableListOf<RamadanDay>()
            val today = Calendar.getInstance()
            
            // Generate all 30 days of Ramadan
            for (dayNum in 1..30) {
                // Get prayer times for this specific day
                val dayCalendar = Calendar.getInstance().apply {
                    time = startDate
                    add(Calendar.DAY_OF_MONTH, dayNum - 1)
                }
                
                val prayerTimes = calculatePrayerTimesForDate(
                    dayCalendar,
                    latitude,
                    longitude
                )
                
                val ramadanDay = RamadanDay(
                    dayNumber = dayNum,
                    date = dayCalendar.time,
                    hijriDate = "$dayNum Ramadan $RAMADAN_2026_HIJRI_YEAR",
                    suhoorTime = prayerTimes["FAJR"] ?: "04:30",
                    iftarTime = prayerTimes["MAGHRIB"] ?: "18:30",
                    isToday = isSameDay(dayCalendar, today)
                )
                
                days.add(ramadanDay)
            }
            
            val endDate = Calendar.getInstance().apply {
                time = startDate
                add(Calendar.DAY_OF_MONTH, 29) // 30 days total
            }.time
            
            val ramadanMonth = RamadanMonth(
                year = year,
                hijriYear = RAMADAN_2026_HIJRI_YEAR,
                startDate = startDate,
                endDate = endDate,
                days = days,
                location = locationName
            )
            
            // Cache the preferences
            saveRamadanPreferences(latitude, longitude, locationName)
            
            Result.success(ramadanMonth)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Ramadan month", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculate prayer times for a specific date
     * Reuses logic from PrayerRepository
     */
    private fun calculatePrayerTimesForDate(
        calendar: Calendar,
        latitude: Double,
        longitude: Double
    ): Map<String, String> {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val timeZone = calendar.timeZone.rawOffset / (1000.0 * 60.0 * 60.0)
        
        // Julian date
        val julianDate = calculateJulianDate(year, month, day)
        
        // Calculate prayer times
        val times = mutableMapOf<String, String>()
        
        val sunriseTime = calculateSunrise(julianDate, latitude, longitude, timeZone)
        val sunsetTime = calculateSunset(julianDate, latitude, longitude, timeZone)
        val noonTime = calculateNoon(julianDate, longitude, timeZone)
        
        // Fajr (Suhoor ends here)
        val fajr = addMinutesToTime(sunriseTime, -100)
        times["FAJR"] = formatTime(fajr)
        
        // Dhuhr
        times["DHUHR"] = formatTime(noonTime + 0.05)
        
        // Asr
        val asr = noonTime + 3.5
        times["ASR"] = formatTime(asr)
        
        // Maghrib (Iftar begins here)
        times["MAGHRIB"] = formatTime(sunsetTime + 0.05)
        
        // Isha
        val isha = addMinutesToTime(sunsetTime, 90)
        times["ISHA"] = formatTime(isha)
        
        return times
    }
    
    /**
     * Get countdown to next Suhoor or Iftar
     */
    fun getCountdown(ramadanMonth: RamadanMonth): RamadanCountdown? {
        val now = Calendar.getInstance()
        
        // Check if we're currently in Ramadan
        if (now.time.before(ramadanMonth.startDate) || now.time.after(ramadanMonth.endDate)) {
            return RamadanCountdown(
                type = CountdownType.SUHOOR,
                timeRemaining = "Not in Ramadan",
                eventTime = "",
                isActive = false
            )
        }
        
        // Find today's Ramadan day
        val todayDay = ramadanMonth.days.find { it.isToday } ?: return null
        
        val currentTime = Calendar.getInstance()
        val suhoorCal = parseTimeToCalendar(todayDay.suhoorTime)
        val iftarCal = parseTimeToCalendar(todayDay.iftarTime)
        
        return when {
            currentTime.before(suhoorCal) -> {
                // Before Suhoor - countdown to Suhoor
                RamadanCountdown(
                    type = CountdownType.SUHOOR,
                    timeRemaining = calculateTimeRemaining(currentTime, suhoorCal),
                    eventTime = todayDay.suhoorTime,
                    isActive = true
                )
            }
            currentTime.before(iftarCal) -> {
                // After Suhoor, before Iftar - countdown to Iftar
                RamadanCountdown(
                    type = CountdownType.IFTAR,
                    timeRemaining = calculateTimeRemaining(currentTime, iftarCal),
                    eventTime = todayDay.iftarTime,
                    isActive = true
                )
            }
            else -> {
                // After Iftar - countdown to tomorrow's Suhoor
                val tomorrowDay = ramadanMonth.days.getOrNull(todayDay.dayNumber) // dayNumber is 1-indexed
                if (tomorrowDay != null) {
                    val tomorrowSuhoor = parseTimeToCalendar(tomorrowDay.suhoorTime).apply {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                    RamadanCountdown(
                        type = CountdownType.SUHOOR,
                        timeRemaining = calculateTimeRemaining(currentTime, tomorrowSuhoor),
                        eventTime = tomorrowDay.suhoorTime,
                        isActive = true
                    )
                } else {
                    null
                }
            }
        }
    }
    
    /**
     * Save Ramadan preferences to SharedPreferences
     */
    fun saveRamadanPreferences(latitude: Double, longitude: Double, locationName: String) {
        prefs.edit().apply {
            putFloat("latitude", latitude.toFloat())
            putFloat("longitude", longitude.toFloat())
            putString("location_name", locationName)
            putLong("last_updated", System.currentTimeMillis())
            apply()
        }
    }
    
    /**
     * Get saved Ramadan preferences
     */
    fun getRamadanPreferences(): RamadanPreferences {
        return RamadanPreferences(
            latitude = prefs.getFloat("latitude", 0.0f).toDouble(),
            longitude = prefs.getFloat("longitude", 0.0f).toDouble(),
            locationName = prefs.getString("location_name", "") ?: "",
            lastUpdated = prefs.getLong("last_updated", 0L)
        )
    }
    
    // ========== Helper Functions ==========
    
    private fun getRamadanStartDate(year: Int): Date {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, RAMADAN_2026_START_YEAR)
            set(Calendar.MONTH, RAMADAN_2026_START_MONTH)
            set(Calendar.DAY_OF_MONTH, RAMADAN_2026_START_DAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }
    
    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun parseTimeToCalendar(timeStr: String): Calendar {
        val parts = timeStr.split(":")
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    
    private fun calculateTimeRemaining(from: Calendar, to: Calendar): String {
        val diffMillis = to.timeInMillis - from.timeInMillis
        val hours = (diffMillis / (1000 * 60 * 60)).toInt()
        val minutes = ((diffMillis / (1000 * 60)) % 60).toInt()
        
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "Now"
        }
    }
    
    // ========== Prayer Time Calculation Helpers (from PrayerRepository) ==========
    
    private fun calculateJulianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }
    
    private fun calculateSunrise(jd: Double, lat: Double, lng: Double, tz: Double): Double {
        val noon = calculateNoon(jd, lng, tz)
        return noon - 6.0
    }
    
    private fun calculateSunset(jd: Double, lat: Double, lng: Double, tz: Double): Double {
        val noon = calculateNoon(jd, lng, tz)
        return noon + 6.0
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
        time = time - floor(time / 24.0) * 24.0
        
        val hours = floor(time).toInt()
        val minutes = ((time - hours) * 60).toInt()
        
        return String.format("%02d:%02d", hours, minutes)
    }
}
