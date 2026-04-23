package com.example.al_aalim.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.al_aalim.auth.FirebaseAuthManager
import com.example.al_aalim.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String, val userName: String? = null) : AuthState()
    data class UnverifiedEmail(val email: String, val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val repository: AuthRepository,
    private val profileRepository: com.example.al_aalim.repository.ProfileRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val isUserLoggedIn: Boolean get() = repository.isUserLoggedIn

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.signInWithEmail(email, password)
            handleAuthResult(result)
        }
    }

    fun registerWithEmail(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.registerWithEmail(email, password, name)
            handleAuthResult(result)
        }
    }
    
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.sendPasswordResetEmail(email)
            handleAuthResult(result)
        }
    }
    
    fun sendEmailVerification(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            // Need password to re-authenticate and send verification email
            val result = repository.sendEmailVerification(email, password)
            handleAuthResult(result)
        }
    }

    fun getGoogleSignInIntent(): Intent? = repository.getGoogleSignInIntent()

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = repository.handleGoogleSignInResult(data)
            handleAuthResult(result)
        }
    }

    private fun handleAuthResult(result: FirebaseAuthManager.AuthResult) {
        when (result) {
            is FirebaseAuthManager.AuthResult.Success -> {
                val userName = when {
                    !result.user.displayName.isNullOrEmpty() -> result.user.displayName
                    !result.user.email.isNullOrEmpty() -> {
                        result.user.email!!.substringBefore("@").replaceFirstChar { it.uppercase() }
                    }
                    else -> "User"
                }
                
                // Save user to profile repository automatically upon login/register
                profileRepository.setCurrentUser(result.user.uid)
                try {
                    profileRepository.saveUserName(result.user.uid, userName!!)
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Error saving user name: ${e.message}", e)
                }
                
                _authState.value = AuthState.Success(result.user.uid, userName)
            }
            is FirebaseAuthManager.AuthResult.UnverifiedEmail -> {
                _authState.value = AuthState.UnverifiedEmail(result.user.email ?: "", result.message)
            }
            is FirebaseAuthManager.AuthResult.Error -> {
                _authState.value = AuthState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
