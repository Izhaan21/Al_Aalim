package com.example.al_aalim.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.al_aalim.repository.AuthRepository
import com.example.al_aalim.repository.ChatRepository
import com.example.al_aalim.repository.SettingsRepository
import com.example.al_aalim.repository.UserDataRepository
import com.example.al_aalim.viewmodel.SettingsViewModel

class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(ChatRepository(context), context.applicationContext) as T
        }
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(AuthRepository(context), com.example.al_aalim.repository.ProfileRepository(context)) as T
        }
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(AuthRepository(context), ChatRepository(context)) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(SettingsRepository(context)) as T
        }
        if (modelClass.isAssignableFrom(AccountViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AccountViewModel(
                AuthRepository(context),
                com.example.al_aalim.repository.ProfileRepository(context),
                UserDataRepository(context),
                ChatRepository(context)
            ) as T
        }
        throw java.lang.IllegalArgumentException("Unknown ViewModel class")
    }
}
