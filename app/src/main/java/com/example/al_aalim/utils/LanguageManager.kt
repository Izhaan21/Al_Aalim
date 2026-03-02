package com.example.al_aalim.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageManager {
    
    private const val PREF_NAME = "app_settings"
    private const val KEY_LANGUAGE = "selected_language"
    private const val DEFAULT_LANGUAGE = "en"
    
    /**
     * Get currently selected language code
     */
    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    /**
     * Save selected language
     */
    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    /**
     * Apply language to context
     */
    fun applyLanguage(context: Context, languageCode: String = getSelectedLanguage(context)): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Get language name in native script
     */
    fun getLanguageNativeName(code: String): String {
        return when (code) {
            "ar" -> "العربية"
            "en" -> "English"
            "ur" -> "اردو"
            "bn" -> "বাংলা"
            "hi" -> "हिन्दी"
            "id" -> "Bahasa Indonesia"
            "ms" -> "Bahasa Melayu"
            "ha" -> "Hausa"
            "sw" -> "Kiswahili"
            "fa" -> "فارسی"
            "tr" -> "Türkçe"
            "fr" -> "Français"
            "de" -> "Deutsch"
            "es" -> "Español"
            "uz" -> "O'zbek"
            "zh" -> "中文"
            "ru" -> "Русский"
            else -> "English"
        }
    }
    
    /**
     * Get language name in English
     */
    fun getLanguageEnglishName(code: String): String {
        return when (code) {
            "ar" -> "Arabic"
            "en" -> "English"
            "ur" -> "Urdu"
            "bn" -> "Bengali"
            "hi" -> "Hindi"
            "id" -> "Indonesian"
            "ms" -> "Malay"
            "ha" -> "Hausa"
            "sw" -> "Swahili"
            "fa" -> "Persian"
            "tr" -> "Turkish"
            "fr" -> "French"
            "de" -> "German"
            "es" -> "Spanish"
            "uz" -> "Uzbek"
            "zh" -> "Chinese"
            "ru" -> "Russian"
            else -> "English"
        }
    }
}
