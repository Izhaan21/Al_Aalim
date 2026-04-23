package com.example.al_aalim.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.al_aalim.utils.ProfileManager

class ProfileRepository(private val context: Context) {

    fun saveProfileImage(userId: String, imageUri: Uri): Boolean {
        return ProfileManager.saveProfileImage(context, userId, imageUri)
    }

    fun getProfileImage(userId: String): Bitmap? {
        return ProfileManager.getProfileImage(context, userId)
    }

    fun getUserName(userId: String): String {
        return ProfileManager.getUserName(context, userId)
    }

    fun saveUserName(userId: String, name: String) {
        ProfileManager.saveUserName(context, userId, name)
    }

    fun getUsername(userId: String): String {
        return ProfileManager.getUsername(context, userId)
    }

    fun saveUsername(userId: String, username: String) {
        ProfileManager.saveUsername(context, userId, username)
    }

    fun getUserInitials(userId: String): String {
        return ProfileManager.getUserInitials(context, userId)
    }

    fun clearCurrentUser() {
        ProfileManager.clearCurrentUser(context)
    }

    fun setCurrentUser(userId: String) {
        ProfileManager.setCurrentUser(context, userId)
    }

    /**
     * Delete all local profile data for a user (used on account deletion).
     */
    fun deleteUserProfile(userId: String): Boolean {
        return ProfileManager.deleteUserProfile(context, userId)
    }

    fun saveGender(userId: String, gender: String) {
        // Save locally as fallback
        val prefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        prefs.edit().putString("${userId}_gender", gender).apply()
        
        // Save to Firestore for cross-device sync
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(userId)
        
        val data = mapOf("gender" to gender)
        userRef.set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener { e ->
                android.util.Log.e("ProfileRepository", "Error saving gender to Firestore", e)
            }
    }

    fun getGender(userId: String, onResult: (String?) -> Unit) {
        // First try to get from Firestore
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val gender = document.getString("gender")
                if (gender != null) {
                    // Update local cache
                    val prefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    prefs.edit().putString("${userId}_gender", gender).apply()
                    onResult(gender)
                } else {
                    // Fallback to local if not in Firestore
                    val prefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                    onResult(prefs.getString("${userId}_gender", null))
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ProfileRepository", "Error getting gender from Firestore", e)
                // Fallback to local
                val prefs = context.getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                onResult(prefs.getString("${userId}_gender", null))
            }
    }
}
