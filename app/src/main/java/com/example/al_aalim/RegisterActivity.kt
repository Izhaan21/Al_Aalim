package com.example.al_aalim

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.al_aalim.auth.FirebaseAuthManager
import com.example.al_aalim.databinding.ActivityRegisterBinding
import com.example.al_aalim.utils.ProfileManager
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var authManager: FirebaseAuthManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth Manager
        authManager = FirebaseAuthManager(this)
        
        // Enable full edge-to-edge display (content under status bar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize View Binding
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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
        // Back button
        binding.ivBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        
        // Create Account button
        binding.btnCreateAccount.setOnClickListener {
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
                            performRegistration()
                        }
                        .start()
                }
                .start()
        }
        
        // Login link
        binding.tvLogin.setOnClickListener {
            navigateToLogin()
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
        
        // Confirm password visibility toggle
        var isConfirmPasswordVisible = false
        binding.ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            if (isConfirmPasswordVisible) {
                // Show password
                binding.etConfirmPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye)
            } else {
                // Hide password
                binding.etConfirmPassword.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_off)
            }
            // Move cursor to end
            binding.etConfirmPassword.setSelection(binding.etConfirmPassword.text.length)
        }
    }
    
    private fun performRegistration() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()
        
        // Validate inputs
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Perform Firebase registration
        showLoading(true)
        
        lifecycleScope.launch {
            val result = authManager.registerWithEmail(email, password, name)
            showLoading(false)
            
            when (result) {
                is FirebaseAuthManager.AuthResult.Success -> {
                    val userId = result.user.uid
                    
                    // Set this as the current active user
                    ProfileManager.setCurrentUser(this@RegisterActivity, userId)
                    
                    // Save user name with user ID
                    try {
                        ProfileManager.saveUserName(this@RegisterActivity, userId, name)
                    } catch (e: Exception) {
                        android.util.Log.e("RegisterActivity", "Error saving user name: ${e.message}", e)
                    }
                    
                    Toast.makeText(
                        this@RegisterActivity,
                        "Account created successfully! Welcome, $name",
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToMain()
                }
                is FirebaseAuthManager.AuthResult.Error -> {
                    Toast.makeText(this@RegisterActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    
    private fun showLoading(show: Boolean) {
        binding.btnCreateAccount.isEnabled = !show
        binding.btnCreateAccount.alpha = if (show) 0.6f else 1f
        // You can add a progress bar to the layout and show/hide it here
    }
    
    private fun animateViews() {
        // Fade in animation for content
        val views = listOf(
            binding.tvWelcome,
            binding.tvTitle,
            binding.tvSubtitle,
            binding.inputName,
            binding.inputEmail,
            binding.inputPassword,
            binding.inputConfirmPassword,
            binding.btnCreateAccount
        )
        
        views.forEachIndexed { index, view ->
            view.alpha = 0f
            view.translationY = 30f
            view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .setStartDelay((index * 60).toLong())
                .start()
        }
        
        // Animate mosque
        animateMosque()
    }
    
    private fun animateMosque() {
        binding.ivMosqueSilhouette.apply {
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(0.45f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(300)
                .withEndAction {
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
                        startFloatingAnimation()
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
    
    private fun navigateToLogin() {
        finish() // Go back to LoginActivity
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right)
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
}
