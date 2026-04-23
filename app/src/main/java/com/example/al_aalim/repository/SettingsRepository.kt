package com.example.al_aalim.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.example.al_aalim.utils.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _selectedReciterId = MutableStateFlow(prefs.getString("selected_reciter", "ar.alafasy") ?: "ar.alafasy")
    val selectedReciterId: StateFlow<String> = _selectedReciterId.asStateFlow()

    fun getSelectedReciterId(): kotlinx.coroutines.flow.Flow<String> = selectedReciterId
    
    fun setSelectedReciterId(id: String) {
        prefs.edit().putString("selected_reciter", id).apply()
        _selectedReciterId.value = id
    }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", true)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()

    var appTheme: Int
        get() = prefs.getInt("app_theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) {
            prefs.edit().putInt("app_theme", value).apply()
            AppCompatDelegate.setDefaultNightMode(value)
        }

    var quranReciter: String
        get() = prefs.getString("quran_reciter", "Mishary Rashid Alafasy") ?: "Mishary Rashid Alafasy"
        set(value) = prefs.edit().putString("quran_reciter", value).apply()

    var quranScript: String
        get() = prefs.getString("quran_script", "Indopak") ?: "Indopak"
        set(value) = prefs.edit().putString("quran_script", value).apply()
        
    fun getLanguage(context: Context): String = LanguageManager.getSelectedLanguage(context)
    fun setLanguage(context: Context, langCode: String) {
        LanguageManager.setLanguage(context, langCode)
    }

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    var locationPermissionGranted: Boolean
        get() = prefs.getBoolean("location_permission_granted", false)
        set(value) = prefs.edit().putBoolean("location_permission_granted", value).apply()
}
