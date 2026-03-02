package com.example.al_aalim

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.al_aalim.databinding.ActivityMoreBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.utils.LanguageManager

class MoreActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMoreBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMoreBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Setup clicks
        setupHeaderActions()
        setupPrayerWorshipClicks()
        setupCommunityClicks()
        setupBottomNavigation()
    }
    
    private fun setupHeaderActions() {
        binding.ivBackHeader.setOnClickWithAnimation {
            finish()
        }
    }
    
    private fun setupPrayerWorshipClicks() {
        // Prayer Tracker - Navigate to PrayerTrackingFragment
        binding.btnPrayerTracker.setOnClickWithAnimation {
            // For now, show message - will implement fragment navigation later
            Toast.makeText(this, "Prayer Tracker - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Tasbih Counter
        binding.btnTasbih.setOnClickWithAnimation {
            Toast.makeText(this, "Tasbih Counter - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Daily Dhikr
        binding.btnDhikr.setOnClickWithAnimation {
            Toast.makeText(this, "Daily Dhikr & Azkar - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Dua Collections
        binding.btnDuas.setOnClickWithAnimation {
            Toast.makeText(this, "Dua Collections - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Ramadan Calendar
        binding.btnRamadan.setOnClickWithAnimation {
            val intent = Intent(this, RamadanActivity::class.java)
            startActivity(intent)
        }
    }
    private fun setupCommunityClicks() {
        // Nearby Mosques
        binding.btnMosques.setOnClickWithAnimation {
            Toast.makeText(this, "Nearby Mosques - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Islamic Events
        binding.btnEvents.setOnClickWithAnimation {
            Toast.makeText(this, "Islamic Events - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Halal Restaurants
        binding.btnHalal.setOnClickWithAnimation {
            Toast.makeText(this, "Halal Restaurants - Coming Soon!", Toast.LENGTH_SHORT).show()
        }
    }

    
    private fun setupBottomNavigation() {
        // Get bottom navigation views
        val navHome = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_home)
        val navQibla = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_qibla)
        val navBook = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_book)
        
        // Set More as active
        setActiveNavigation(3) // 3 means More is active
        
        // Home navigation
        navHome?.setOnClickWithAnimation {
            val intent = Intent(this, ContainerActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        
        // Qibla navigation
        navQibla?.setOnClickWithAnimation {
            val intent = Intent(this, QiblaActivity::class.java)
            startActivity(intent)
            finish()
        }
        
        // Quran navigation
        navBook?.setOnClickWithAnimation {
            val intent = Intent(this, QuranActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun setActiveNavigation(position: Int) {
        val navHome = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_home)
        val navQibla = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_qibla)
        val navBook = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_book)
        
        val ivHome = binding.bottomNavigation.findViewById<android.widget.ImageView>(R.id.iv_nav_home)
        val ivQibla = binding.bottomNavigation.findViewById<android.widget.ImageView>(R.id.iv_nav_qibla)
        val ivBook = binding.bottomNavigation.findViewById<android.widget.ImageView>(R.id.iv_nav_book)
        
        val tvHome = binding.bottomNavigation.findViewById<android.widget.TextView>(R.id.tv_nav_home)
        val tvQibla = binding.bottomNavigation.findViewById<android.widget.TextView>(R.id.tv_nav_qibla)
        val tvBook = binding.bottomNavigation.findViewById<android.widget.TextView>(R.id.tv_nav_book)
        
        // Reset all
        navHome?.isSelected = false
        navQibla?.isSelected = false
        navBook?.isSelected = false
        
        ivHome?.isSelected = false
        ivQibla?.isSelected = false
        ivBook?.isSelected = false
        
        tvHome?.isSelected = false
        tvQibla?.isSelected = false
        tvBook?.isSelected = false

        // Reset More selection
        val navMore = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_more)
        val ivMore = binding.bottomNavigation.findViewById<android.widget.ImageView>(R.id.iv_nav_more)
        val tvMore = binding.bottomNavigation.findViewById<android.widget.TextView>(R.id.tv_nav_more)
        navMore?.isSelected = false
        ivMore?.isSelected = false
        tvMore?.isSelected = false
        
        // Set active based on position
        when (position) {
            0 -> {
                navHome?.isSelected = true
                ivHome?.isSelected = true
                tvHome?.isSelected = true
            }
            1 -> {
                navQibla?.isSelected = true
                ivQibla?.isSelected = true
                tvQibla?.isSelected = true
            }
            2 -> {
                navBook?.isSelected = true
                ivBook?.isSelected = true
                tvBook?.isSelected = true
            }
            3 -> {
                // More is active
                val navMore = binding.bottomNavigation.findViewById<android.widget.LinearLayout>(R.id.nav_more)
                val ivMore = binding.bottomNavigation.findViewById<android.widget.ImageView>(R.id.iv_nav_more)
                val tvMore = binding.bottomNavigation.findViewById<android.widget.TextView>(R.id.tv_nav_more)
                
                navMore?.isSelected = true
                ivMore?.isSelected = true
                tvMore?.isSelected = true
            }
        }
        // Position -1 or any other value means everything is unselected
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}
