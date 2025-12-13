package com.example.al_aalim

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.databinding.ActivityQiblaBinding

class QiblaActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityQiblaBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize View Binding
        binding = ActivityQiblaBinding.inflate(layoutInflater)
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
        
        // Handle window insets for the main layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }
        
        // Hide system bars initially
        hideSystemBars()
        
        // Setup navigation
        setupNavigation()
        
        // Setup back button using View Binding
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // Set Qibla as active
        setActiveNavigation()
    }
    
    private fun setActiveNavigation() {
        // Set Qibla as selected
        binding.navQibla.isSelected = true
        binding.ivNavQibla.isSelected = true
        binding.tvNavQibla.isSelected = true
        
        // Ensure others are not selected
        binding.navHome.isSelected = false
        binding.ivNavHome.isSelected = false
        binding.tvNavHome.isSelected = false
        
        binding.navBook.isSelected = false
        binding.ivNavBook.isSelected = false
        binding.tvNavBook.isSelected = false
    }
    
    private fun setupNavigation() {
        // Home navigation using View Binding
        binding.navHome.setOnClickListener {
            finish() // Go back to MainActivity
        }
        
        // Qibla navigation (current screen, do nothing)
        binding.navQibla.setOnClickListener {
            // Already on Qibla screen
        }
        
        // Book navigation
        binding.navBook.setOnClickListener {
            val intent = Intent(this, QuranActivity::class.java)
            startActivity(intent)
            finish()
        }
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
