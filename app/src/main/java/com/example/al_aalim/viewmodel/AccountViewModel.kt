package com.example.al_aalim.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.al_aalim.model.BookmarkedVerse
import com.example.al_aalim.model.ReadingHistoryItem
import com.example.al_aalim.repository.AuthRepository
import com.example.al_aalim.repository.ProfileRepository
import com.example.al_aalim.repository.UserDataRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import com.example.al_aalim.repository.ChatRepository

data class UserProfileState(
    val name: String = "",
    val username: String = "",
    val initials: String = "",
    val profileImage: Bitmap? = null,
    val gender: String? = null,
    val updateTime: Long = 0L
)

class AccountViewModel(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val userDataRepository: UserDataRepository,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _profileState = MutableStateFlow(UserProfileState())
    val profileState: StateFlow<UserProfileState> = _profileState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _messageEvent = MutableStateFlow<String?>(null)
    val messageEvent: StateFlow<String?> = _messageEvent.asStateFlow()

    fun clearMessage() {
        _messageEvent.value = null
    }

    private val _authEvent = MutableStateFlow<AuthEvent?>(null)
    val authEvent: StateFlow<AuthEvent?> = _authEvent.asStateFlow()

    val currentUser = authRepository.currentUser

    val isGoogleUser: Boolean
        get() = currentUser?.providerData?.any { it.providerId == "google.com" } == true

    fun loadProfile() {
        val userId = currentUser?.uid ?: return
        viewModelScope.launch {
            var name = profileRepository.getUserName(userId)

            // If local name is still the default placeholder, seed from Firebase Auth displayName
            if ((name == "New User" || name.isBlank()) && !currentUser?.displayName.isNullOrBlank()) {
                name = currentUser!!.displayName!!
                profileRepository.saveUserName(userId, name) // persist locally
            }

            val username = profileRepository.getUsername(userId)
            val initials = profileRepository.getUserInitials(userId)
            val profileImage = profileRepository.getProfileImage(userId)
            
            // Set initial state with existing data
            _profileState.value = UserProfileState(
                name = name,
                username = username,
                initials = initials,
                profileImage = profileImage,
                gender = _profileState.value.gender, // keep current gender until loaded
                updateTime = System.currentTimeMillis()
            )
            
            // Load gender asynchronously
            profileRepository.getGender(userId) { gender ->
                _profileState.value = _profileState.value.copy(gender = gender)
            }
        }
    }

    fun saveProfile(name: String) {
        val userId = currentUser?.uid ?: return
        if (name.isNotEmpty()) {
            // Save display name locally
            profileRepository.saveUserName(userId, name)

            // Also update Firebase Auth displayName so it persists across sessions
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            currentUser?.updateProfile(profileUpdates)
                ?.addOnFailureListener { e ->
                    android.util.Log.e("AccountViewModel", "Failed to update Firebase displayName", e)
                }

            loadProfile()
            _messageEvent.value = "Profile saved!"
        } else {
            _messageEvent.value = "Name cannot be empty"
        }
    }

    fun updateProfileImage(uri: Uri) {
        val userId = currentUser?.uid ?: return
        val saved = profileRepository.saveProfileImage(userId, uri)
        if (saved) {
            loadProfile()
            _messageEvent.value = "Profile photo updated!"
        } else {
            _messageEvent.value = "Failed to save profile photo"
        }
    }

    fun setGender(gender: String) {
        val userId = currentUser?.uid ?: return
        profileRepository.saveGender(userId, gender)
        loadProfile()
        _messageEvent.value = "Gender updated to $gender"
    }

    fun logout() {
        profileRepository.clearCurrentUser()
        authRepository.signOut()
        _authEvent.value = AuthEvent.LoggedOut
    }

    fun changePassword(current: String, new: String, confirm: String) {
        if (current.isEmpty() || new.isEmpty() || confirm.isEmpty()) {
            _messageEvent.value = "Please fill all fields"
            return
        }
        if (new != confirm) {
            _messageEvent.value = "Passwords don't match"
            return
        }
        if (new.length < 6) {
            _messageEvent.value = "Password must be at least 6 characters"
            return
        }

        _isLoading.value = true
        val email = currentUser?.email ?: return
        authRepository.reauthenticate(email, current) { reauthSuccess, reauthMsg ->
            if (reauthSuccess) {
                authRepository.updatePassword(new) { updateSuccess, updateMsg ->
                    _isLoading.value = false
                    if (updateSuccess) {
                        _messageEvent.value = "Password changed successfully"
                        _authEvent.value = AuthEvent.PasswordChanged
                    } else {
                        _messageEvent.value = "Failed to change password: $updateMsg"
                    }
                }
            } else {
                _isLoading.value = false
                _messageEvent.value = "Current password is incorrect"
            }
        }
    }

    fun changeEmail(password: String, newEmail: String) {
        if (!isGoogleUser && password.isEmpty()) {
            _messageEvent.value = "Please fill all fields"
            return
        }
        if (newEmail.isEmpty()) {
            _messageEvent.value = "Please fill all fields"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _messageEvent.value = "Please enter a valid email"
            return
        }

        _isLoading.value = true
        val currentEmail = currentUser?.email ?: return
        
        if (isGoogleUser) {
            authRepository.updateEmail(newEmail) { updateSuccess, updateMsg ->
                _isLoading.value = false
                if (updateSuccess) {
                    _messageEvent.value = "Verification email sent to $newEmail. Please click the link in your email to confirm."
                    _authEvent.value = AuthEvent.EmailChanged
                } else {
                    android.util.Log.e("AccountViewModel", "Failed to send verification email: $updateMsg")
                    _messageEvent.value = "Failed to send verification email: $updateMsg"
                }
            }
        } else {
            authRepository.reauthenticate(currentEmail, password) { reauthSuccess, _ ->
                if (reauthSuccess) {
                    authRepository.updateEmail(newEmail) { updateSuccess, updateMsg ->
                        _isLoading.value = false
                        if (updateSuccess) {
                            _messageEvent.value = "Verification email sent to $newEmail. Please click the link in your email to confirm."
                            _authEvent.value = AuthEvent.EmailChanged
                        } else {
                            android.util.Log.e("AccountViewModel", "Failed to send verification email: $updateMsg")
                            _messageEvent.value = "Failed to send verification email: $updateMsg"
                        }
                    }
                } else {
                    _isLoading.value = false
                    _messageEvent.value = "Password is incorrect"
                }
            }
        }
    }

    fun clearAuthEvent() {
        _authEvent.value = null
    }
    
    /**
     * Deletes the current Firebase Auth account after re-authentication.
     * Cleans up all local profile data and calls [onComplete] on success.
     */
    fun deleteAccount(password: String, onComplete: () -> Unit) {
        val user = currentUser ?: return
        val email = user.email ?: return
        val userId = user.uid

        _isLoading.value = true
        
        val proceedWithDeletion = {
            user.delete().addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    // Clean up local data
                    profileRepository.clearCurrentUser()
                    profileRepository.deleteUserProfile(userId)
                    authRepository.signOut()
                    _authEvent.value = AuthEvent.AccountDeleted
                    onComplete()
                } else {
                    _messageEvent.value = "Failed to delete account: ${task.exception?.message}"
                }
            }
        }

        if (isGoogleUser) {
            proceedWithDeletion()
        } else {
            authRepository.reauthenticate(email, password) { reauthSuccess, reauthMsg ->
                if (reauthSuccess) {
                    proceedWithDeletion()
                } else {
                    _isLoading.value = false
                    _messageEvent.value = "Incorrect password. Please try again."
                }
            }
        }
    }

    // ─────────────────────────── Bookmarks ───────────────────────────────

    private val _bookmarks = MutableStateFlow<List<BookmarkedVerse>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkedVerse>> = _bookmarks.asStateFlow()

    fun loadBookmarks() {
        val userId = currentUser?.uid ?: return
        _bookmarks.value = userDataRepository.getBookmarks(userId)
    }

    fun removeBookmark(surahNumber: Int, verseNumber: Int) {
        val userId = currentUser?.uid ?: return
        userDataRepository.removeBookmark(userId, surahNumber, verseNumber)
        loadBookmarks()
    }

    fun clearBookmarks() {
        val userId = currentUser?.uid ?: return
        userDataRepository.clearBookmarks(userId)
        loadBookmarks()
    }

    // ─────────────────────────── Reading History ──────────────────────────

    private val _readingHistory = MutableStateFlow<List<ReadingHistoryItem>>(emptyList())
    val readingHistory: StateFlow<List<ReadingHistoryItem>> = _readingHistory.asStateFlow()

    fun loadReadingHistory() {
        val userId = currentUser?.uid ?: return
        _readingHistory.value = userDataRepository.getReadingHistory(userId)
    }

    fun clearReadingHistory() {
        val userId = currentUser?.uid ?: return
        userDataRepository.clearReadingHistory(userId)
        loadReadingHistory()
    }

    fun removeHistoryItem(surahNumber: Int, readAt: Long) {
        val userId = currentUser?.uid ?: return
        userDataRepository.removeHistoryItem(userId, surahNumber, readAt)
        loadReadingHistory()
    }

    // ─────────────────────────── App Data ───────────────────────────────

    fun deleteChatHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                chatRepository.deleteAllConversations()
            }
            _isLoading.value = false
            if (result.isSuccess) {
                _messageEvent.value = "All chat history has been deleted."
            } else {
                _messageEvent.value = "Failed to delete history: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearCache(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    context.cacheDir.deleteRecursively()
                } catch (e: Exception) {
                    android.util.Log.e("AccountViewModel", "Error clearing cache", e)
                }
            }
            _messageEvent.value = "App cache cleared successfully"
        }
    }
}

sealed class AuthEvent {
    object LoggedOut : AuthEvent()
    object PasswordChanged : AuthEvent()
    object EmailChanged : AuthEvent()
    object AccountDeleted : AuthEvent()
}
