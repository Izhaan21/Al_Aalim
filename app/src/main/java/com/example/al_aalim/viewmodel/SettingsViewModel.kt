package com.example.al_aalim.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.al_aalim.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _notificationsEnabled = MutableStateFlow(repository.notificationsEnabled)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _appTheme = MutableStateFlow(repository.appTheme)
    val appTheme: StateFlow<Int> = _appTheme.asStateFlow()

    private val _quranReciter = MutableStateFlow(repository.quranReciter)
    val quranReciter: StateFlow<String> = _quranReciter.asStateFlow()

    private val _quranReciterId = MutableStateFlow(repository.selectedReciterId.value)
    val quranReciterId: StateFlow<String> = _quranReciterId.asStateFlow()

    private val _quranScript = MutableStateFlow(repository.quranScript)
    val quranScript: StateFlow<String> = _quranScript.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        repository.notificationsEnabled = enabled
        _notificationsEnabled.value = enabled
    }

    fun setTheme(theme: Int) {
        repository.appTheme = theme
        _appTheme.value = theme
    }

    fun setQuranReciter(reciter: String) {
        repository.quranReciter = reciter
        _quranReciter.value = reciter
    }

    fun setQuranReciterId(reciterId: String) {
        repository.setSelectedReciterId(reciterId)
        _quranReciterId.value = reciterId
    }

    fun setQuranScript(script: String) {
        repository.quranScript = script
        _quranScript.value = script
    }
    
    fun getLanguage(context: Context): String = repository.getLanguage(context)
    
    fun setLanguage(context: Context, langCode: String) {
        repository.setLanguage(context, langCode)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        repository.onboardingCompleted = completed
    }

    fun getOnboardingCompleted(): Boolean {
        return repository.onboardingCompleted
    }

    fun setLocationPermissionGranted(granted: Boolean) {
        repository.locationPermissionGranted = granted
    }

    fun getLocationPermissionGranted(): Boolean {
        return repository.locationPermissionGranted
    }
}
