package com.example.al_aalim

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.al_aalim.auth.FirebaseAuthManager
import com.example.al_aalim.databinding.ActivityLoginBinding
import com.example.al_aalim.utils.ProfileManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var authManager: FirebaseAuthManager
    
    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            lifecycleScope.launch {
                showLoading(true)
                val authResult = authManager.handleGoogleSignInResult(result.data)
                showLoading(false)
                handleAuthResult(authResult)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth Manager
        authManager = FirebaseAuthManager(this)
        
        // Check if user is already logged in
        if (authManager.isUserLoggedIn) {
            navigateToMain()
            return
        }
        
        // Enable full edge-to-edge display (content under status bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize View Binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configure system bars for immersive mode
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        
        // Set transparent system bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // For Android 10+ (API 29+), enable gesture navigation edge-to-edge
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        // Hide action bar for cleaner look
        supportActionBar?.hide()
        
        // Hide system bars initially
        hideSystemBars()
        
        // Setup button click listeners
        setupClickListeners()
        
        // Add subtle animations
        animateViews()
    }
    
    private fun setupClickListeners() {
        // Login button - Email/Password
        binding.btnLogin.setOnClickListener {
            // Add ripple effect feel
            it.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction {
                    it.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            performEmailLogin()
                        }
                        .start()
                }
                .start()
        }
        
        // Google Sign-In
        binding.btnGoogle.setOnClickListener {
            performGoogleSignIn()
        }
        
        // Facebook Sign-In
        binding.btnFacebook.setOnClickListener {
            performFacebookSignIn()
        }
        
        // Apple Sign-In (placeholder - requires additional setup)
        binding.btnApple.setOnClickListener {
            Toast.makeText(this, "Apple Sign-In requires additional configuration", Toast.LENGTH_SHORT).show()
        }
        
        // Create Account button
        binding.btnCreateAccount.setOnClickListener {
            navigateToRegister()
        }
        
        // Forgot password
        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
        
        // Password visibility toggle
        var isPasswordVisible = false
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                // Show password
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye)
            } else {
                // Hide password
                binding.etPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            }
            // Move cursor to end
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }
    
    private fun performEmailLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            return
        }
        
        showLoading(true)
        
        lifecycleScope.launch {
            val result = authManager.signInWithEmail(email, password)
            showLoading(false)
            handleAuthResult(result)
        }
    }
    
    private fun performGoogleSignIn() {
        val signInIntent = authManager.getGoogleSignInIntent()
        if (signInIntent != null) {
            googleSignInLauncher.launch(signInIntent)
        } else {
            Toast.makeText(this, "Google Sign-In not configured", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performFacebookSignIn() {
        showLoading(true)
        authManager.signInWithFacebook { result ->
            showLoading(false)
            handleAuthResult(result)
        }
    }
    
    private fun handleAuthResult(result: FirebaseAuthManager.AuthResult) {
        when (result) {
            is FirebaseAuthManager.AuthResult.Success -> {
                val userId = result.user.uid
                
                // Set this as the current active user
                ProfileManager.setCurrentUser(this, userId)
                
                // Save user's display name with smart fallback logic
                val userName = when {
                    !result.user.displayName.isNullOrEmpty() -> result.user.displayName!!
                    !result.user.email.isNullOrEmpty() -> {
                        result.user.email!!.substringBefore("@").replaceFirstChar { it.uppercase() }
                    }
                    else -> "User"
                }
                
                try {
                    ProfileManager.saveUserName(this, userId, userName)
                    navigateToMain()
                } catch (e: Exception) {
                    android.util.Log.e("LoginActivity", "Error saving user name: ${e.message}", e)
                    navigateToMain()
                }
            }
            is FirebaseAuthManager.AuthResult.Error -> {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showForgotPasswordDialog() {
        val input = EditText(this).apply {
            hint = "Enter your email"
            setPadding(50, 30, 50, 30)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setMessage("Enter your email address to receive a password reset link")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun sendPasswordResetEmail(email: String) {
        lifecycleScope.launch {
            showLoading(true)
            val result = authManager.sendPasswordResetEmail(email)
            showLoading(false)
            
            when (result) {
                is FirebaseAuthManager.AuthResult.Success -> {
                    Toast.makeText(
                        this@LoginActivity,
                        "Password reset email sent! Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                is FirebaseAuthManager.AuthResult.Error -> {
                    Toast.makeText(this@LoginActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.btnLogin.isEnabled = !show
        binding.btnLogin.alpha = if (show) 0.6f else 1f
        // You can add a progress bar to the layout and show/hide it here
    }
    
    private fun animateViews() {
        // Animate illustration with scale and fade
        binding.ivIllustration.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .start()
        }
        
        // Animate glass card sliding up
        binding.glassCard.apply {
            alpha = 0f
            translationY = 100f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setStartDelay(200)
                .start()
        }
        
        // Fade in animation for content inside glass card
        val views = listOf(
            binding.tvWelcome,
            binding.tvSubtitle,
            binding.inputEmail,
            binding.inputPassword,
            binding.btnLogin,
            binding.dividerContainer,
            binding.socialButtonsContainer,
            binding.createAccountContainer
        )
        
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.animate()
                .alpha(1f)
                .setDuration(300)
                .setStartDelay((400 + index * 60).toLong())
                .start()
        }
        
        // Animate mosque silhouette - gentle floating effect
        animateMosque()
    }
    
    private fun animateMosque() {
        binding.ivMosqueSilhouette.apply {
            // Initial fade in
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(0.5f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(300)
                .withEndAction {
                    // Start continuous gentle floating animation
                    startFloatingAnimation()
                }
                .start()
        }
    }
    
    private fun startFloatingAnimation() {
        binding.ivMosqueSilhouette.animate()
            .translationY(-8f)
            .setDuration(2500)
            .withEndAction {
                binding.ivMosqueSilhouette.animate()
                    .translationY(0f)
                    .setDuration(2500)
                    .withEndAction {
                        startFloatingAnimation() // Loop the animation
                    }
                    .start()
            }
            .start()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, LocationPermissionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
    
    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Handle Facebook callback
        authManager.onActivityResult(requestCode, resultCode, data)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }
    
    private fun hideSystemBars() {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.al_aalim.utils.LanguageManager.applyLanguage(newBase))
    }
}
