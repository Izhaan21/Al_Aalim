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
    private lateinit var loadingOverlay: View
    private lateinit var blurOverlay: View
    private lateinit var tvLoadingStatus: TextView
    private lateinit var btnBackLoading: View
    private lateinit var centeredContainer: View
    private lateinit var settingsNote: TextView
    private lateinit var glassCard: View
    private lateinit var ivMosqueSilhouette: View
    private lateinit var ivIllustration: View
    
    // Prayer beads container (for rotation)
    private lateinit var prayerBeadsContainer: View
    
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
        
        // Check if location permission is already granted
        // if (ContextCompat.checkSelfPermission(
        //         this,
        //         Manifest.permission.ACCESS_FINE_LOCATION
        //     ) == PackageManager.PERMISSION_GRANTED
        // ) {
        //     // Permission already granted, skip to language selection
        //     // navigateToMain()
        //     // return
        // }
        
        initViews()
        setupClickListeners()
        animateViews()
    }

    private fun initViews() {
        btnAllowLocation = findViewById(R.id.btn_allow_location)
        btnSkip = findViewById(R.id.btn_skip)
        loadingOverlay = findViewById(R.id.loading_overlay)
        blurOverlay = findViewById(R.id.blur_overlay)
        tvLoadingStatus = findViewById(R.id.tv_loading_status)
        btnBackLoading = findViewById(R.id.btn_back_loading)
        centeredContainer = findViewById(R.id.centered_container)
        prayerBeadsContainer = findViewById(R.id.prayer_beads_container)
        settingsNote = findViewById(R.id.settings_note)
        glassCard = findViewById(R.id.glass_card)
        ivMosqueSilhouette = findViewById(R.id.iv_mosque_silhouette)
        ivIllustration = findViewById(R.id.iv_illustration)
    }

    private fun setupClickListeners() {
        btnAllowLocation.setOnClickListener {
            requestLocationPermission()
        }

        btnSkip.setOnClickListener {
            navigateToMain()
        }

        btnBackLoading.setOnClickListener {
            // Cancel location fetch and hide loading
            LocationManager.stopLocationUpdates()
            locationListener?.let { LocationManager.removeLocationListener(it) }
            hideLoading()
        }
    }

    private fun requestLocationPermission() {
        // First check if location services are enabled
        if (!isLocationEnabled()) {
            showLocationServicesDisabledDialog()
            return
        }
        
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
    
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }
    
    private fun showLocationServicesDisabledDialog() {
        showBottomSheetDialog(
            title = "📍 Enable Location Services",
            message = "Location services are currently turned off.\n\n" +
                    "To use Al-Aalim's location features like Qibla direction and prayer times, please:\n\n" +
                    "1. Tap 'Open Settings'\n" +
                    "2. Turn on 'Location' or 'GPS'\n" +
                    "3. Return to Al-Aalim",
            positiveText = "Open Settings",
            negativeText = "Skip",
            onPositive = { openLocationSettings() },
            onNegative = { navigateToMain() }
        )
    }

    private fun showPermissionDeniedDialog() {
        showBottomSheetDialog(
            title = "📍 Location Access Required",
            message = "Location is essential for the best Al-Aalim experience:\n\n" +
                    "🕋 Accurate Qibla Direction\n" +
                    "Find the exact direction to Mecca from anywhere in the world.\n\n" +
                    "🕌 Prayer Times\n" +
                    "Get precise prayer times based on your current location.\n\n" +
                    "Without location access, you'll need to manually select your location each time.",
            positiveText = "Try Again",
            negativeText = "Skip",
            onPositive = { 
                hasRequestedPermission = true
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onNegative = { navigateToMain() }
        )
    }

    private fun showPermanentlyDeniedDialog() {
        showBottomSheetDialog(
            title = "📍 Permission Required",
            message = "You have permanently denied location access.\n\n" +
                    "To enable location for accurate Qibla direction and prayer times, please:\n\n" +
                    "1. Tap 'Open Settings'\n" +
                    "2. Go to 'Permissions'\n" +
                    "3. Enable 'Location'",
            positiveText = "Open Settings",
            negativeText = "Skip",
            onPositive = { openAppSettings() },
            onNegative = { navigateToMain() }
        )
    }

    private fun showPermissionRationaleDialog() {
        showBottomSheetDialog(
            title = "📍 Why We Need Location",
            message = "Al-Aalim uses your location to:\n\n" +
                    "• Show accurate Qibla direction\n" +
                    "• Calculate prayer times for your area\n" +
                    "• Provide location-based Islamic services\n\n" +
                    "Your location data stays on your device and is never shared.",
            positiveText = "Allow Location",
            negativeText = "Not Now",
            onPositive = { 
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onNegative = { navigateToMain() }
        )
    }

    private fun showBottomSheetDialog(
        title: String,
        message: String,
        positiveText: String,
        negativeText: String,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_permission, null)
        
        view.findViewById<TextView>(R.id.tv_title).text = title
        view.findViewById<TextView>(R.id.tv_message).text = message
        
        val btnPositive = view.findViewById<TextView>(R.id.btn_positive)
        btnPositive.text = positiveText
        btnPositive.setOnClickListener {
            dialog.dismiss()
            onPositive()
        }
        
        val btnNegative = view.findViewById<TextView>(R.id.btn_negative)
        btnNegative.text = negativeText
        btnNegative.setOnClickListener {
            dialog.dismiss()
            onNegative()
        }
        
        dialog.setContentView(view)
        dialog.show()
    }
    
    private fun openLocationSettings() {
        wentToLocationSettings = true
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
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
        
        // Show loading overlay and hide main content
        loadingOverlay.visibility = View.VISIBLE
        centeredContainer.visibility = View.GONE
        btnAllowLocation.visibility = View.GONE
        btnSkip.visibility = View.GONE
        settingsNote.visibility = View.GONE
        
        tvLoadingStatus.text = "Getting your location..."
        
        // Start prayer beads rotation animation
        startPrayerBeadsAnimation()
        
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
                stopPrayerBeadsAnimation()
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
                
                stopPrayerBeadsAnimation()
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

        val intent = Intent(this, LanguageSelectionActivity::class.java)
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
    
    private var wentToLocationSettings = false
    
    override fun onResume() {
        super.onResume()
        // Only recheck if user went to location settings
        if (wentToLocationSettings && isLocationEnabled()) {
            wentToLocationSettings = false
            // Location services are now enabled, request permission
            hasRequestedPermission = true
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
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
    
    
    private fun startPrayerBeadsAnimation() {
        // ProgressBar animates automatically when visible, no need to start animation
    }
    
    private fun stopPrayerBeadsAnimation() {
        // Show main content again
        centeredContainer.visibility = View.VISIBLE
        btnAllowLocation.visibility = View.VISIBLE
        btnSkip.visibility = View.VISIBLE
        settingsNote.visibility = View.VISIBLE
    }
    
    private fun animateViews() {
        // Animate glass card sliding up
        glassCard.apply {
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
            centeredContainer,
            btnAllowLocation,
            btnSkip,
            settingsNote
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
        ivMosqueSilhouette.apply {
            // Initial fade in
            alpha = 0f
            translationY = 50f
            animate()
                .alpha(0.3f)
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
        ivMosqueSilhouette.animate()
            .translationY(-8f)
            .setDuration(2500)
            .withEndAction {
                ivMosqueSilhouette.animate()
                    .translationY(0f)
                    .setDuration(2500)
                    .withEndAction {
                        startFloatingAnimation() // Loop the animation
                    }
                    .start()
            }
            .start()
    }
    
    private fun showLoading() {
        // Show loading and blur overlay
        loadingOverlay.visibility = View.VISIBLE
        blurOverlay.visibility = View.VISIBLE
        
        // Hide all other content
        ivIllustration.visibility = View.GONE
        glassCard.visibility = View.GONE
        ivMosqueSilhouette.visibility = View.GONE
    }
    
    private fun hideLoading() {
        // Hide loading and blur overlay
        loadingOverlay.visibility = View.GONE
        blurOverlay.visibility = View.GONE
        
        // Show content again
        ivIllustration.visibility = View.VISIBLE
        glassCard.visibility = View.VISIBLE
        ivMosqueSilhouette.visibility = View.VISIBLE
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.al_aalim.utils.LanguageManager.applyLanguage(newBase))
    }
}
