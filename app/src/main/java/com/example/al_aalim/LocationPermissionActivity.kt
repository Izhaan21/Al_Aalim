package com.example.al_aalim

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.al_aalim.utils.LocationManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LocationPermissionActivity : AppCompatActivity() {

    private lateinit var btnAllowLocation: TextView
    private lateinit var btnSkip: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var tvLoadingStatus: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private var locationListener: ((Double, Double) -> Unit)? = null
    
    // Timeout for location fetch (10 seconds)
    private val LOCATION_TIMEOUT_MS = 10000L
    
    // Track if permission was requested before (to detect permanent denial)
    private var hasRequestedPermission = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                saveLocationPermissionGranted()
                startLocationFetchWithProgress()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Approximate location access granted
                saveLocationPermissionGranted()
                startLocationFetchWithProgress()
            }
            else -> {
                // Permission denied - check if permanently denied
                val isPermanentlyDenied = hasRequestedPermission && 
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                
                if (isPermanentlyDenied) {
                    // User checked "Don't ask again" - show dialog with settings option
                    showPermanentlyDeniedDialog()
                } else {
                    // Normal denial - show importance dialog
                    showPermissionDeniedDialog()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_location_permission)

        // Handle system bars insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hideSystemBars()
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        btnAllowLocation = findViewById(R.id.btn_allow_location)
        btnSkip = findViewById(R.id.btn_skip)
        loadingOverlay = findViewById(R.id.loading_overlay)
        tvLoadingStatus = findViewById(R.id.tv_loading_status)
    }

    private fun setupClickListeners() {
        btnAllowLocation.setOnClickListener {
            requestLocationPermission()
        }

        btnSkip.setOnClickListener {
            navigateToMain()
        }
    }

    private fun requestLocationPermission() {
        when {
            // Already have permission
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                saveLocationPermissionGranted()
                startLocationFetchWithProgress()
            }
            // Should show rationale (user denied before but didn't check "Don't ask again")
            ActivityCompat.shouldShowRequestPermissionRationale(
                this, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                showPermissionRationaleDialog()
            }
            // Request permission
            else -> {
                hasRequestedPermission = true
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    private fun showPermissionDeniedDialog() {
        val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
            .setTitle("📍 Location Access Required")
            .setMessage(
                "Location is essential for the best Al-Aalim experience:\n\n" +
                "🕋 Accurate Qibla Direction\n" +
                "Find the exact direction to Mecca from anywhere in the world.\n\n" +
                "🕌 Prayer Times\n" +
                "Get precise prayer times based on your current location.\n\n" +
                "🌙 Islamic Calendar\n" +
                "Accurate moon sighting and Islamic dates for your region.\n\n" +
                "Without location access, you'll need to manually select your location each time."
            )
            .setPositiveButton("Try Again") { dialog, _ ->
                dialog.dismiss()
                hasRequestedPermission = true
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
                navigateToMain()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
        
        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.gold)
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.text_secondary)
        )
    }
    
    private fun showPermanentlyDeniedDialog() {
        val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
            .setTitle("📍 Permission Required")
            .setMessage(
                "You have permanently denied location access.\n\n" +
                "To enable location for accurate Qibla direction and prayer times, please:\n\n" +
                "1. Tap 'Open Settings'\n" +
                "2. Go to 'Permissions'\n" +
                "3. Enable 'Location'"
            )
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Skip") { dialog, _ ->
                dialog.dismiss()
                navigateToMain()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
        
        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.gold)
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.text_secondary)
        )
    }
    
    private fun showPermissionRationaleDialog() {
        val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
            .setTitle("📍 Why We Need Location")
            .setMessage(
                "Al-Aalim uses your location to:\n\n" +
                "• Show accurate Qibla direction\n" +
                "• Calculate prayer times for your area\n" +
                "• Provide location-based Islamic services\n\n" +
                "Your location data stays on your device and is never shared."
            )
            .setPositiveButton("Allow Location") { dialog, _ ->
                dialog.dismiss()
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton("Not Now") { dialog, _ ->
                dialog.dismiss()
                navigateToMain()
            }
            .setCancelable(false)
            .create()
        
        dialog.show()
        
        // Style the buttons
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.gold)
        )
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.text_secondary)
        )
    }
    
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun saveLocationPermissionGranted() {
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("location_permission_granted", true).apply()
    }
    
    private fun startLocationFetchWithProgress() {
        Log.d("LocationPermission", "Starting location fetch with progress")
        
        // Show loading overlay
        loadingOverlay.visibility = View.VISIBLE
        tvLoadingStatus.text = "Getting your location..."
        
        // Initialize LocationManager and start fetching location
        LocationManager.initialize(this)
        LocationManager.startLocationUpdates(this)
        
        // Add listener for location updates
        locationListener = { lat, lon ->
            Log.d("LocationPermission", "Location received: $lat, $lon")
            
            // Remove listener to prevent multiple callbacks
            locationListener?.let { LocationManager.removeLocationListener(it) }
            
            // Update UI on main thread
            handler.post {
                tvLoadingStatus.text = "Location found!"
                
                // Small delay to show success message
                handler.postDelayed({
                    navigateToMain()
                }, 500)
            }
        }
        LocationManager.addLocationListener(locationListener!!)
        
        // Set timeout - if location not received within timeout, proceed anyway
        handler.postDelayed({
            if (loadingOverlay.visibility == View.VISIBLE) {
                Log.d("LocationPermission", "Location timeout - proceeding without location")
                locationListener?.let { LocationManager.removeLocationListener(it) }
                
                tvLoadingStatus.text = "Continuing..."
                handler.postDelayed({
                    navigateToMain()
                }, 300)
            }
        }, LOCATION_TIMEOUT_MS)
    }

    private fun navigateToMain() {
        // Mark that the user has completed onboarding
        val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("onboarding_completed", true).apply()

        val intent = Intent(this, ContainerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up listener
        locationListener?.let { LocationManager.removeLocationListener(it) }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }
}
