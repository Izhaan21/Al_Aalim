package com.example.al_aalim

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.databinding.ActivityWelcomeBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation

class WelcomeActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityWelcomeBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize View Binding
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
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
        
        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }
        
        // Hide system bars initially
        hideSystemBars()
        
        // Setup button click listeners
        setupButtons()
    }
    
    private fun setupButtons() {
        // Get Started button with animation
        binding.btnGetStarted.setOnClickWithAnimation {
            navigateToMain()
        }
        
        // Start animations
        animateViews()
    }
    
    private fun animateViews() {
        // Animate content
        binding.contentContainer.apply {
            alpha = 0f
            translationY = 40f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(200)
                .start()
        }
        
        // Animate mosque
        animateMosque()
        
        // Animate sparkles with twinkling effect
        animateSparkles()
    }
    
    private fun animateMosque() {
        binding.ivMosqueSilhouette.apply {
            alpha = 0f
            translationY = 60f
            animate()
                .alpha(0.5f)
                .translationY(0f)
                .setDuration(800)
                .setStartDelay(400)
                .withEndAction {
                    startFloatingAnimation()
                }
                .start()
        }
    }
    
    private fun startFloatingAnimation() {
        binding.ivMosqueSilhouette.animate()
            .translationY(-10f)
            .setDuration(3000)
            .withEndAction {
                binding.ivMosqueSilhouette.animate()
                    .translationY(0f)
                    .setDuration(3000)
                    .withEndAction {
                        startFloatingAnimation()
                    }
                    .start()
            }
            .start()
    }
    
    private fun animateSparkles() {
        // Left sparkles - gentle pulse
        binding.ivSparklesLeft.apply {
            alpha = 0f
            animate()
                .alpha(0.6f)
                .setDuration(1000)
                .setStartDelay(500)
                .withEndAction { pulseSparkle(this, 0.6f, 0.3f) }
                .start()
        }
        
        // Right sparkles - gentle pulse with offset
        binding.ivSparklesRight.apply {
            alpha = 0f
            animate()
                .alpha(0.5f)
                .setDuration(1000)
                .setStartDelay(800)
                .withEndAction { pulseSparkle(this, 0.5f, 0.2f) }
                .start()
        }
    }
    
    private fun pulseSparkle(view: android.view.View, maxAlpha: Float, minAlpha: Float) {
        view.animate()
            .alpha(minAlpha)
            .setDuration(2000)
            .withEndAction {
                view.animate()
                    .alpha(maxAlpha)
                    .setDuration(2000)
                    .withEndAction { pulseSparkle(view, maxAlpha, minAlpha) }
                    .start()
            }
            .start()
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
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
