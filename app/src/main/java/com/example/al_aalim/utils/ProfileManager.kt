package com.example.al_aalim.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Utility object for managing user profile data including profile photo.
 * Supports per-user storage - each user's profile data is stored separately
 * based on their Firebase UID to persist across login sessions.
 */
object ProfileManager {
    
    private const val PREFS_NAME = "profile_prefs"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    
    /**
     * Set the current active user (call after login)
     */
    fun setCurrentUser(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
    }
    
    /**
     * Get current active user ID
     */
    fun getCurrentUserId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_USER_ID, null)
    }
    
    /**
     * Get user-specific filename for profile image
     */
    private fun getProfileImageFilename(userId: String): String {
        return "profile_image_${userId}.jpg"
    }
    
    /**
     * Get user-specific preference key
     */
    private fun getUserKey(userId: String, baseKey: String): String {
        return "${userId}_${baseKey}"
    }
    
    /**
     * Save profile image from URI to internal storage (user-specific)
     */
    fun saveProfileImage(context: Context, userId: String, imageUri: Uri): Boolean {
        return try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (bitmap != null) {
                saveProfileBitmap(context, userId, bitmap)
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Save profile image bitmap to internal storage (user-specific)
     */
    fun saveProfileBitmap(context: Context, userId: String, bitmap: Bitmap): Boolean {
        return try {
            val file = File(context.filesDir, getProfileImageFilename(userId))
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // Save the path to SharedPreferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(getUserKey(userId, "profile_image_path"), file.absolutePath).apply()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get the saved profile image as Bitmap (user-specific)
     */
    fun getProfileImage(context: Context, userId: String): Bitmap? {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val imagePath = prefs.getString(getUserKey(userId, "profile_image_path"), null)
            
            if (imagePath != null) {
                val file = File(imagePath)
                if (file.exists()) {
                    BitmapFactory.decodeFile(imagePath)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if profile image exists (user-specific)
     */
    fun hasProfileImage(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val imagePath = prefs.getString(getUserKey(userId, "profile_image_path"), null)
        return imagePath != null && File(imagePath).exists()
    }
    
    /**
     * Delete profile image (user-specific)
     */
    fun deleteProfileImage(context: Context, userId: String): Boolean {
        return try {
            val file = File(context.filesDir, getProfileImageFilename(userId))
            if (file.exists()) {
                file.delete()
            }
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(getUserKey(userId, "profile_image_path")).apply()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Save user name (user-specific)
     */
    fun saveUserName(context: Context, userId: String, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(getUserKey(userId, "user_name"), name).apply()
    }
    
    /**
     * Get user name (user-specific)
     */
    fun getUserName(context: Context, userId: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(getUserKey(userId, "user_name"), "New User") ?: "New User"
    }
    
    /**
     * Save username (user-specific)
     */
    fun saveUsername(context: Context, userId: String, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(getUserKey(userId, "user_username"), username).apply()
    }
    
    /**
     * Get username (user-specific)
     */
    fun getUsername(context: Context, userId: String): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(getUserKey(userId, "user_username"), "user") ?: "user"
    }
    
    /**
     * Get user initials from name (user-specific)
     */
    fun getUserInitials(context: Context, userId: String): String {
        val name = getUserName(context, userId)
        val parts = name.trim().split(" ")
        return when {
            parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
            parts.isNotEmpty() -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}"
            else -> "U"
        }
    }
    
    /**
     * Clear current user session (call on logout)
     * This does NOT delete user's profile data, just clears the active session
     */
    fun clearCurrentUser(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }
    
    /**
     * Delete all profile data for a specific user
     * Only use when a user wants to permanently delete their profile
     */
    fun deleteUserProfile(context: Context, userId: String): Boolean {
        return try {
            // Delete profile image file
            val file = File(context.filesDir, getProfileImageFilename(userId))
            if (file.exists()) {
                file.delete()
            }
            
            // Clear user-specific preferences
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.remove(getUserKey(userId, "profile_image_path"))
            editor.remove(getUserKey(userId, "user_name"))
            editor.remove(getUserKey(userId, "user_username"))
            editor.apply()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
