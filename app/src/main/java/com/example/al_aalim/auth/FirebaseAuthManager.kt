package com.example.al_aalim.auth

import android.app.Activity
import android.content.Intent
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginResult
import com.facebook.login.LoginManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import android.content.Context
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication Manager
 * Handles all authentication operations including email/password, Google, and Facebook sign-in
 */
class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    private var facebookCallbackManager: CallbackManager? = null

    /**
     * Sealed class to represent authentication results
     */
    sealed class AuthResult {
        data class Success(val user: FirebaseUser) : AuthResult()
        data class UnverifiedEmail(val user: FirebaseUser, val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    init {
        setupGoogleSignIn()
        setupFacebookSignIn()
    }

    /**
     * Get current authenticated user
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /**
     * Check if user is logged in
     */
    val isUserLoggedIn: Boolean
        get() {
            val user = auth.currentUser
            return user != null && (user.isEmailVerified || isThirdPartySignIn(user))
        }
        
    private fun isThirdPartySignIn(user: FirebaseUser): Boolean {
        return user.providerData.any { profile -> 
            profile.providerId == GoogleAuthProvider.PROVIDER_ID || 
            profile.providerId == FacebookAuthProvider.PROVIDER_ID
        }
    }

    // ==================== Email/Password Authentication ====================

    /**
     * Register new user with email and password
     */
    suspend fun registerWithEmail(
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        return try {
            // Validate inputs
            if (!isValidEmail(email)) {
                return AuthResult.Error("Please enter a valid email address")
            }
            if (password.length < 6) {
                return AuthResult.Error("Password must be at least 6 characters")
            }
            if (displayName.isBlank()) {
                return AuthResult.Error("Please enter your name")
            }

            // Create user
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                // Update display name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                user.updateProfile(profileUpdates).await()
                
                // Send verification email
                user.sendEmailVerification().await()
                
                // Sign out immediately so they must verify and log in
                auth.signOut()

                AuthResult.UnverifiedEmail(user, "Registration successful! Please check your email to verify your account.")
            } else {
                AuthResult.Error("Registration failed. Please try again.")
            }
        } catch (e: Exception) {
            // If the email is already registered, guide the user to verify/login
            if (e.message?.contains("email-already-in-use") == true ||
                e.message?.contains("already registered") == true) {
                // Try to find if account exists but is unverified
                // Return an UnverifiedEmail result so the UI can offer resend/login options
                return AuthResult.Error("This email is already registered. If you haven't verified your email, please check your inbox or use 'Resend Verification Email' on the login screen.")
            }
            AuthResult.Error(e.message ?: "Registration failed. Please try again.")
        }
    }

    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            // Validate inputs
            if (!isValidEmail(email)) {
                return AuthResult.Error("Please enter a valid email address")
            }
            if (password.isBlank()) {
                return AuthResult.Error("Please enter your password")
            }

            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user

            if (user != null) {
                if (user.isEmailVerified || isThirdPartySignIn(user)) {
                    AuthResult.Success(user)
                } else {
                    // Sign out the unverified user
                    auth.signOut()
                    AuthResult.UnverifiedEmail(user, "Please verify your email address to log in.")
                }
            } else {
                AuthResult.Error("Login failed. Please try again.")
            }
        } catch (e: Exception) {
            AuthResult.Error(getErrorMessage(e))
        }
    }
    
    /**
     * Send email verification link
     */
    suspend fun sendEmailVerification(email: String, password: String): AuthResult {
        return try {
            // Need to sign in first to send verification email
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                user.sendEmailVerification().await()
                auth.signOut() // Sign out again
                AuthResult.Error("Verification email sent successfully! Please check your inbox.")
            } else {
                AuthResult.Error("Failed to send verification email.")
            }
        } catch (e: Exception) {
            AuthResult.Error("Failed to send verification email: ${e.message}")
        }
    }

    /**
     * Send password reset email
     */
    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            if (!isValidEmail(email)) {
                return AuthResult.Error("Please enter a valid email address")
            }

            auth.sendPasswordResetEmail(email).await()
            // Return success with current user if available, or create a dummy success
            val user = auth.currentUser
            if (user != null) {
                AuthResult.Success(user)
            } else {
                // Password reset email sent successfully, but no user is logged in
                // This is expected behavior - return error with success message
                AuthResult.Error("Password reset email sent successfully!")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Failed to send reset email")
        }
    }

    // ==================== Google Sign-In ====================

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Web Client ID - Get this from Firebase Console:
            // 1. Go to Firebase Console → Authentication → Sign-in method
            // 2. Enable Google
            // 3. Expand the Google provider
            // 4. Copy the "Web SDK configuration" Web client ID
            // 5. Replace the string below with your actual Web client ID
            .requestIdToken("1000133801969-nrg2s1887cnpf4n7hj2iakj5d2mvf3fe.apps.googleusercontent.com")
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    /**
     * Get Google Sign-In intent
     */
    fun getGoogleSignInIntent(): Intent? {
        return googleSignInClient?.signInIntent
    }

    /**
     * Handle Google Sign-In result
     */
    suspend fun handleGoogleSignInResult(data: Intent?): AuthResult {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            AuthResult.Error("Google sign-in failed: ${e.message}")
        } catch (e: Exception) {
            AuthResult.Error("Google sign-in failed: ${e.message}")
        }
    }

    private suspend fun firebaseAuthWithGoogle(account: GoogleSignInAccount): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user

            if (user != null) {
                AuthResult.Success(user)
            } else {
                AuthResult.Error("Google sign-in failed")
            }
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Google sign-in failed")
        }
    }

    // ==================== Facebook Sign-In ====================

    private fun setupFacebookSignIn() {
        facebookCallbackManager = CallbackManager.Factory.create()
    }

    /**
     * Sign in with Facebook
     */
    fun signInWithFacebook(activity: android.app.Activity, callback: (AuthResult) -> Unit) {
        val loginManager = LoginManager.getInstance()
        
        loginManager.registerCallback(facebookCallbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                handleFacebookAccessToken(result.accessToken, callback)
            }

            override fun onCancel() {
                callback(AuthResult.Error("Facebook sign-in cancelled"))
            }

            override fun onError(error: FacebookException) {
                callback(AuthResult.Error("Facebook sign-in failed: ${error.message}"))
            }
        })

        loginManager.logInWithReadPermissions(activity, listOf("email", "public_profile"))
    }

    private fun handleFacebookAccessToken(token: AccessToken, callback: (AuthResult) -> Unit) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    callback(AuthResult.Success(user))
                } else {
                    callback(AuthResult.Error("Facebook sign-in failed"))
                }
            }
            .addOnFailureListener { e ->
                callback(AuthResult.Error(e.message ?: "Facebook sign-in failed"))
            }
    }

    /**
     * Handle Facebook callback result
     */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        facebookCallbackManager?.onActivityResult(requestCode, resultCode, data)
    }

    // ==================== Sign Out ====================

    /**
     * Sign out from all providers
     */
    fun signOut() {
        auth.signOut()
        googleSignInClient?.signOut()
        LoginManager.getInstance().logOut()
    }

    // ==================== Helper Methods ====================

    /**
     * Validate email format
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Get user-friendly error messages
     */
    private fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("no user record") == true ||
            exception.message?.contains("wrong-password") == true ||
            exception.message?.contains("invalid-credential") == true ->
                "Invalid email or password. Please check your credentials."
            exception.message?.contains("network") == true ->
                "Network error. Please check your internet connection."
            exception.message?.contains("too-many-requests") == true ->
                "Too many attempts. Please try again later."
            exception.message?.contains("user-disabled") == true ->
                "This account has been disabled."
            else -> exception.message ?: "Authentication failed. Please try again."
        }
    }
}
