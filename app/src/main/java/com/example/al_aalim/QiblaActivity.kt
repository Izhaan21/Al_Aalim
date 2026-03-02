package com.example.al_aalim

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.databinding.ActivityQiblaBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.utils.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.*

class QiblaActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var binding: ActivityQiblaBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)
    
    private var currentAzimuth = 0f
    private var qiblaBearing = 0f
    private var userLatitude = 0.0
    private var userLongitude = 0.0
    private var hasLocation = false
    
    // Location selection
    private var selectedLocationIndex = -1
    
    // Mecca coordinates
    private val MECCA_LATITUDE = 21.4225
    private val MECCA_LONGITUDE = 39.8262
    
    companion object {
        private const val TAG = "QiblaActivity"
    }

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            Log.d(TAG, "Location permission granted")
            startLocationTracking()
        } else {
            Log.d(TAG, "Location permission denied")
            Toast.makeText(
                this,
                "Location permission is required for Qibla direction",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate started")

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

        // Initialize location and sensors
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        Log.d(TAG, "Accelerometer: ${accelerometer != null}, Magnetometer: ${magnetometer != null}")

        // Setup navigation
        setupNavigation()

        // Setup back button using View Binding
        binding.ivBack.setOnClickListener {
            finish()
        }

        // Set Qibla as active
        setActiveNavigation()

        // Check if LocationManager has cached location from permission screen
        if (LocationManager.hasLocation) {
            Log.d(TAG, "Using cached location from LocationManager")
            userLatitude = LocationManager.userLatitude
            userLongitude = LocationManager.userLongitude
            hasLocation = true
            
            // Update coordinates display
            binding.tvCoordinates.text = String.format(
                "%.6f, %.6f",
                userLatitude,
                userLongitude
            )
            
            // Calculate Qibla direction immediately
            calculateQibla()
        } else {
            // No cached location - user needs to select from bottom sheet
            Log.d(TAG, "No cached location available")
        }
        
        // Setup Choose Location button
        binding.btnChooseLocation.setOnClickListener {
            showLocationBottomSheet()
        }
        
        Log.d(TAG, "onCreate completed")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - registering sensors")
        
        // Register sensor listeners
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Accelerometer registered")
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
            Log.d(TAG, "Magnetometer registered")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - unregistering sensors")
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // Low-pass filter
                    val alpha = 0.97f
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    // Low-pass filter
                    val alpha = 0.97f
                    geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * it.values[0]
                    geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * it.values[1]
                    geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * it.values[2]
                }
            }

            // Calculate orientation
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientation)
                
                // Get azimuth in degrees
                var azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                
                // Normalize to 0-360
                azimuthInDegrees = (azimuthInDegrees + 360) % 360
                
                currentAzimuth = azimuthInDegrees
                
                // Update compass rotation
                updateCompassRotation()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }

    private fun updateCompassRotation() {
        // Rotate compass needle to point north
        // Negative because Android rotation is clockwise
        val compassRotation = -currentAzimuth
        
        // Rotate Kaaba to point toward Qibla direction
        val qiblaRotation = qiblaBearing - currentAzimuth
        
        binding.ivCompassNeedle.rotation = compassRotation
        binding.ivKaabaIndicator.rotation = qiblaRotation
        
        // Update angle display - show how many degrees to rotate to face Qibla
        if (hasLocation) {
            val angleToQibla = (qiblaBearing - currentAzimuth + 360) % 360
            binding.tvAngle.text = String.format("%.0f°", angleToQibla)
        }
    }

    private fun checkLocationPermission() {
        Log.d(TAG, "Checking location permission")
        
        val fineLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission already granted")
            startLocationTracking()
        } else {
            Log.d(TAG, "Requesting permission")
            // Request permissions
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationTracking() {
        Log.d(TAG, "Starting location tracking")
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            // Get current location
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d(TAG, "Location received: ${location.latitude}, ${location.longitude}")
                    userLatitude = location.latitude
                    userLongitude = location.longitude
                    hasLocation = true
                    
                    // Update coordinates display
                    binding.tvCoordinates.text = String.format(
                        "%.6f, %.6f",
                        userLatitude,
                        userLongitude
                    )
                    
                    Toast.makeText(
                        this,
                        "Location: ${userLatitude.format(4)}, ${userLongitude.format(4)}",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // Calculate Qibla direction and distance
                    calculateQibla()
                } else {
                    Log.d(TAG, "Location is null")
                    Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Location failed: ${exception.message}")
                Toast.makeText(this, "Location error: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.d(TAG, "No permission for location")
        }
    }
    
    private fun Double.format(digits: Int) = "%.${digits}f".format(this)

    private fun calculateQibla() {
        Log.d(TAG, "Calculating Qibla direction")
        
        // Calculate bearing to Mecca
        qiblaBearing = calculateBearing(
            userLatitude,
            userLongitude,
            MECCA_LATITUDE,
            MECCA_LONGITUDE
        ).toFloat()
        
        Log.d(TAG, "Qibla bearing: $qiblaBearing degrees")
        
        // Calculate distance to Mecca
        val distance = calculateDistance(
            userLatitude,
            userLongitude,
            MECCA_LATITUDE,
            MECCA_LONGITUDE
        )
        
        Log.d(TAG, "Distance to Mecca: $distance km")
        
        // Update distance display
        binding.tvDistance.text = String.format("%.1fKM", distance)
        
        Toast.makeText(
            this,
            "Qibla: ${qiblaBearing.toInt()}° | Distance: ${distance.toInt()}km",
            Toast.LENGTH_LONG
        ).show()
        
        // Update compass rotation
        updateCompassRotation()
    }

    private fun calculateBearing(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        
        var bearing = Math.toDegrees(atan2(y, x))
        bearing = (bearing + 360) % 360
        
        return bearing
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6371.0 // km
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
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

        binding.navMore.isSelected = false
        binding.ivNavMore.isSelected = false
        binding.tvNavMore.isSelected = false
    }

    private fun setupNavigation() {
        // Home navigation using View Binding
        binding.navHome.setOnClickWithAnimation {
            finish() // Go back to MainActivity
        }

        // Qibla navigation (current screen, do nothing)
        binding.navQibla.setOnClickWithAnimation {
            // Already on Qibla screen
        }

        // Book navigation
        binding.navBook.setOnClickWithAnimation {
            val intent = Intent(this, QuranActivity::class.java)
            startActivity(intent)
            finish()
        }

        // More navigation
        binding.navMore.setOnClickWithAnimation {
            val intent = Intent(this, MoreActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun showLocationBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_location, null)
        
        val rvLocations = bottomSheetView.findViewById<RecyclerView>(R.id.rv_locations)
        val btnChoose = bottomSheetView.findViewById<CardView>(R.id.btn_choose)
        val btnUseMyLocation = bottomSheetView.findViewById<View>(R.id.btn_use_my_location)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.et_search_location)
        val skeletonContainer = bottomSheetView.findViewById<View>(R.id.skeleton_container)
        
        // Reset selection
        selectedLocationIndex = -1
        
        // Setup RecyclerView with empty list initially
        rvLocations.layoutManager = LinearLayoutManager(this)
        val adapter = CountryLocationAdapter(mutableListOf()) { position ->
            selectedLocationIndex = position
        }
        rvLocations.adapter = adapter
        
        // Initially hide list, skeleton and choose button
        rvLocations.visibility = View.GONE
        skeletonContainer.visibility = View.GONE
        btnChoose.visibility = View.GONE
        
        // Search handler with debounce
        var searchJob: android.os.Handler? = android.os.Handler(android.os.Looper.getMainLooper())
        var searchRunnable: Runnable? = null
        
        // Search filtering
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // Cancel previous search
                searchRunnable?.let { searchJob?.removeCallbacks(it) }
                
                val query = s.toString().trim()
                
                if (query.isEmpty()) {
                    // Hide list, skeleton and choose button when search is empty
                    rvLocations.visibility = View.GONE
                    skeletonContainer.visibility = View.GONE
                    btnChoose.visibility = View.GONE
                    adapter.updateLocations(mutableListOf())
                    selectedLocationIndex = -1
                } else {
                    // Show skeleton loading
                    skeletonContainer.visibility = View.VISIBLE
                    rvLocations.visibility = View.GONE
                    btnChoose.visibility = View.GONE
                    
                    // Debounce search by 300ms
                    searchRunnable = Runnable {
                        val results = com.example.al_aalim.data.CountriesData.searchCountries(query)
                        
                        // Hide skeleton, show results and choose button
                        skeletonContainer.visibility = View.GONE
                        rvLocations.visibility = View.VISIBLE
                        btnChoose.visibility = View.VISIBLE
                        
                        adapter.updateLocations(results.toMutableList())
                        selectedLocationIndex = -1
                    }
                    searchJob?.postDelayed(searchRunnable!!, 300)
                }
            }
        })
        
        // Choose button click
        btnChoose.setOnClickListener {
            if (selectedLocationIndex >= 0 && selectedLocationIndex < adapter.locations.size) {
                val selectedLocation = adapter.locations[selectedLocationIndex]
                
                userLatitude = selectedLocation.latitude
                userLongitude = selectedLocation.longitude
                hasLocation = true
                
                binding.tvCoordinates.text = String.format("%.6f, %.6f", userLatitude, userLongitude)
                calculateQibla()
                
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Use My Location click
        btnUseMyLocation.setOnClickListener {
            bottomSheetDialog.dismiss()
            checkLocationPermission()
        }
        
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Configure bottom sheet - start collapsed, expand when searching
        bottomSheetDialog.behavior.apply {
            skipCollapsed = false
            isFitToContents = true
            isHideable = true
        }
        
        // Set window soft input mode to adjust resize so content stays visible above keyboard
        bottomSheetDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        // Focus on search field when shown
        bottomSheetDialog.setOnShowListener {
            etSearch.requestFocus()
            // Show keyboard automatically
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        bottomSheetDialog.show()
    }
    
    // Country Location Adapter inner class
    inner class CountryLocationAdapter(
        var locations: MutableList<com.example.al_aalim.data.CountryLocation>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<CountryLocationAdapter.ViewHolder>() {
        
        private var selectedPos = -1
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvFlag: TextView = view.findViewById(R.id.tv_flag)
            val tvLocationName: TextView = view.findViewById(R.id.tv_location_name)
            val ivSelected: ImageView = view.findViewById(R.id.iv_selected)
            val locationCard: CardView = view.findViewById(R.id.location_card)
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_location, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val location = locations[position]
            holder.tvFlag.text = location.flag
            holder.tvLocationName.text = "${location.name}, ${location.capital}"
            
            // Show selection state
            if (selectedPos == position) {
                holder.ivSelected.visibility = View.VISIBLE
                holder.locationCard.setCardBackgroundColor(ContextCompat.getColor(this@QiblaActivity, R.color.background_light_gray))
            } else {
                holder.ivSelected.visibility = View.GONE
                holder.locationCard.setCardBackgroundColor(ContextCompat.getColor(this@QiblaActivity, R.color.white))
            }
            
            holder.itemView.setOnClickListener {
                val previousPos = selectedPos
                selectedPos = holder.adapterPosition
                notifyItemChanged(previousPos)
                notifyItemChanged(selectedPos)
                onItemClick(selectedPos)
            }
        }
        
        override fun getItemCount() = locations.size
        
        fun updateLocations(newLocations: MutableList<com.example.al_aalim.data.CountryLocation>) {
            locations = newLocations
            selectedPos = -1
            notifyDataSetChanged()
        }
    }
}

