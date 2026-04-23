package com.example.al_aalim.repository

import android.content.Context
import android.content.Intent
import com.example.al_aalim.auth.FirebaseAuthManager

class AuthRepository(context: Context) {

    private val authManager = FirebaseAuthManager(context)

    val isUserLoggedIn: Boolean get() = authManager.isUserLoggedIn
    
    val currentUser get() = authManager.currentUser

    suspend fun signInWithEmail(email: String, password: String): FirebaseAuthManager.AuthResult =
        authManager.signInWithEmail(email, password)

    suspend fun registerWithEmail(email: String, password: String, name: String): FirebaseAuthManager.AuthResult =
        authManager.registerWithEmail(email, password, name)

    suspend fun sendPasswordResetEmail(email: String): FirebaseAuthManager.AuthResult =
        authManager.sendPasswordResetEmail(email)
        
    suspend fun sendEmailVerification(email: String, password: String): FirebaseAuthManager.AuthResult =
        authManager.sendEmailVerification(email, password)

    fun getGoogleSignInIntent(): Intent? = authManager.getGoogleSignInIntent()

    suspend fun handleGoogleSignInResult(data: Intent?): FirebaseAuthManager.AuthResult =
        authManager.handleGoogleSignInResult(data)

    fun signInWithFacebook(activity: android.app.Activity, callback: (FirebaseAuthManager.AuthResult) -> Unit) {
        authManager.signInWithFacebook(activity, callback)
    }

    fun onFacebookActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        authManager.onActivityResult(requestCode, resultCode, data)
    }

    fun signOut() {
        authManager.signOut()
    }

    fun reauthenticate(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        val user = currentUser ?: return onResult(false, "No user logged in")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun updatePassword(newPassword: String, onResult: (Boolean, String?) -> Unit) {
        val user = currentUser ?: return onResult(false, "No user logged in")
        user.updatePassword(newPassword).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }

    fun updateEmail(newEmail: String, onResult: (Boolean, String?) -> Unit) {
        val user = currentUser ?: return onResult(false, "No user logged in")
        // Use verifyBeforeUpdateEmail instead of updateEmail for better security and to support 
        // projects with Email Enumeration Protection enabled.
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Note: The email is not actually changed until the user clicks the link in their email.
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message)
            }
        }
    }
}
