package com.example.al_aalim

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.al_aalim.databinding.FragmentQiblaBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.example.al_aalim.utils.LocationManager
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.*

class QiblaFragment : Fragment(), SensorEventListener, OnMapReadyCallback {
    
    private var _binding: FragmentQiblaBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var locationCallback: LocationCallback? = null
    
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
    
    // Track if we already vibrated to avoid continuous vibration
    private var hasVibrated = false
    
    // Mecca coordinates
    private val MECCA_LATITUDE = 21.4225
    private val MECCA_LONGITUDE = 39.8262
    
    // Threshold for "phone is flat" detection (in degrees)
    private val FLAT_THRESHOLD = 25f
    // Threshold for "Qibla found" (in degrees)
    private val QIBLA_FOUND_THRESHOLD = 5f
    
    // Google Maps variables
    private var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private var userMarker: Marker? = null
    private var meccaMarker: Marker? = null
    private var qiblaPolyline: Polyline? = null
    
    companion object {
        private const val TAG = "QiblaFragment"
    }
    
    // Location selection
    private var selectedLocationIndex = -1
    
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
                requireContext(),
                "Location permission is required for Qibla direction",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQiblaBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated started")
        
        // Initialize location and sensors
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        
        Log.d(TAG, "Accelerometer: ${accelerometer != null}, Magnetometer: ${magnetometer != null}")
        
        // Initialize MapView
        mapView = binding.mapView
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        
        // Setup back button with animation
        binding.ivBack.setOnClickWithAnimation {
            (activity as? ContainerActivity)?.navigateToPage(0)
        }
        
        // Setup expand map button to open Google Maps with animation
        binding.ivExpandMap.setOnClickWithAnimation {
            openGoogleMaps()
        }
        
        // Check if LocationManager has cached location from permission screen
        if (LocationManager.hasLocation) {
            Log.d(TAG, "Using cached location from LocationManager")
            userLatitude = LocationManager.userLatitude
            userLongitude = LocationManager.userLongitude
            hasLocation = true
            
            // Update coordinates display
            binding.tvCoordinates.text = String.format(
                "%.7f, %.7f",
                userLatitude,
                userLongitude
            )
            
            // Calculate Qibla direction immediately
            calculateQibla()
            
            // Update map markers when map is ready
            if (googleMap != null) {
                updateMapMarkers()
            }
        } else {
            // No cached location - user needs to select from bottom sheet
            Log.d(TAG, "No cached location available")
        }
        
        // Setup Choose Location button with animation
        binding.btnChooseLocation.setOnClickWithAnimation {
            showLocationBottomSheet()
        }
        
        Log.d(TAG, "onViewCreated completed")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume - registering sensors")
        
        // Resume map
        mapView?.onResume()
        
        // Register sensor listeners
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause - unregistering sensors")
        mapView?.onPause()
        sensorManager.unregisterListener(this)
        stopLocationUpdates()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()
        stopLocationUpdates()
        _binding = null
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        Log.d(TAG, "Map is ready")
        
        // Configure map settings
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isZoomGesturesEnabled = true
            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            mapType = GoogleMap.MAP_TYPE_NORMAL
        }
        
        // Add Mecca marker immediately
        addMeccaMarker()
        
        // If we already have location, update the map
        if (hasLocation) {
            updateMapMarkers()
        }
    }
    
    private fun addMeccaMarker() {
        val meccaLatLng = LatLng(MECCA_LATITUDE, MECCA_LONGITUDE)
        
        // Create custom Kaaba icon from drawable
        val kaabaIcon = getBitmapFromDrawable(R.drawable.ic_kaaba_marker, 80, 80)
        
        meccaMarker = googleMap?.addMarker(
            MarkerOptions()
                .position(meccaLatLng)
                .title("Mecca (Kaaba)")
                .icon(if (kaabaIcon != null) BitmapDescriptorFactory.fromBitmap(kaabaIcon) 
                      else BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        
        // Initially zoom to Mecca
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(meccaLatLng, 3f))
    }
    
    private fun getBitmapFromDrawable(drawableId: Int, width: Int, height: Int): Bitmap? {
        return try {
            val drawable = ContextCompat.getDrawable(requireContext(), drawableId) ?: return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error creating bitmap from drawable: ${e.message}")
            null
        }
    }
    
    private fun openGoogleMaps() {
        try {
            // Create intent to open Google Maps with directions to Mecca
            val uri = if (hasLocation) {
                // If we have user location, show directions
                android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&origin=$userLatitude,$userLongitude&destination=$MECCA_LATITUDE,$MECCA_LONGITUDE&travelmode=driving")
            } else {
                // Otherwise just show Mecca location
                android.net.Uri.parse("geo:$MECCA_LATITUDE,$MECCA_LONGITUDE?q=$MECCA_LATITUDE,$MECCA_LONGITUDE(Kaaba)")
            }
            
            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
            mapIntent.setPackage("com.google.android.apps.maps")
            
            if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // If Google Maps not installed, open in browser
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Google Maps: ${e.message}")
            Toast.makeText(requireContext(), "Could not open maps", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateMapMarkers() {
        if (googleMap == null || !hasLocation) return
        
        val userLatLng = LatLng(userLatitude, userLongitude)
        val meccaLatLng = LatLng(MECCA_LATITUDE, MECCA_LONGITUDE)
        
        // Update or create user marker
        if (userMarker == null) {
            userMarker = googleMap?.addMarker(
                MarkerOptions()
                    .position(userLatLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        } else {
            userMarker?.position = userLatLng
        }
        
        // Update or create polyline from user to Mecca
        qiblaPolyline?.remove()
        qiblaPolyline = googleMap?.addPolyline(
            PolylineOptions()
                .add(userLatLng, meccaLatLng)
                .width(4f)
                .color(android.graphics.Color.parseColor("#00897B"))
                .geodesic(true) // Great circle path
        )
        
        // Zoom to show both markers
        try {
            val bounds = LatLngBounds.Builder()
                .include(userLatLng)
                .include(meccaLatLng)
                .build()
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: Exception) {
            Log.e(TAG, "Error zooming to bounds: ${e.message}")
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (_binding == null) return
        
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // Use higher alpha for smoother response (reduces fluctuation)
                    val alpha = if (gravity[0] == 0f && gravity[1] == 0f && gravity[2] == 0f) 0f else 0.95f
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]
                    
                    // Check if phone is flat
                    checkPhoneFlat()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    // Use higher alpha for smoother response (reduces fluctuation)
                    val alpha = if (geomagnetic[0] == 0f && geomagnetic[1] == 0f && geomagnetic[2] == 0f) 0f else 0.95f
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
    
    private fun checkPhoneFlat() {
        if (_binding == null) return
        
        // Calculate pitch and roll from gravity
        val pitch = Math.toDegrees(atan2(gravity[1].toDouble(), gravity[2].toDouble())).toFloat()
        val roll = Math.toDegrees(atan2(gravity[0].toDouble(), gravity[2].toDouble())).toFloat()
        
        // Phone is flat when both pitch and roll are close to 0 (or 180 for face down)
        val isFlat = abs(pitch) < FLAT_THRESHOLD && abs(roll) < FLAT_THRESHOLD
        
        // Show/hide instruction text based on flat detection
        binding.tvInstruction.visibility = if (isFlat) View.INVISIBLE else View.VISIBLE
    }
    
    private fun updateCompassRotation() {
        if (_binding == null) return
        
        // Calculate rotation for the inner compass
        // Rotate so that when user faces Qibla, Kaaba icon points to top (triangle)
        val compassRotation = -(currentAzimuth - qiblaBearing)
        
        // Rotate only the inner rotating container (outer ring and triangle stay fixed)
        binding.rotatingCompass.rotation = compassRotation
        
        // Counter-rotate Kaaba icon to keep it upright/vertical
        binding.ivKaabaIndicator.rotation = -compassRotation
        
        // Update angle display
        if (hasLocation) {
            // Calculate how far user needs to rotate to face Qibla
            var angleToQibla = qiblaBearing - currentAzimuth
            // Normalize to -180 to 180
            while (angleToQibla > 180) angleToQibla -= 360
            while (angleToQibla < -180) angleToQibla += 360
            
            val displayAngle = abs(angleToQibla).toInt()
            binding.tvAngle.text = String.format("%d°", displayAngle)
            
            // Update rotation guidance text
            val rotationGuidance = when {
                displayAngle == 0 -> "Qibla found! ✓"
                displayAngle <= 5 -> "Almost there!"
                angleToQibla > 0 -> "Rotate ${displayAngle}° to the right"
                else -> "Rotate ${displayAngle}° to the left"
            }
            binding.tvRotationGuidance.text = rotationGuidance
            
            // Check if Qibla is found (when angle shows 0°)
            if (displayAngle == 0) {
                // Change outer ring and triangle to gold color when Qibla found
                binding.ivOuterRing.setColorFilter(android.graphics.Color.parseColor("#FFD700"))
                binding.ivTriangle.setColorFilter(android.graphics.Color.parseColor("#FFD700"))
                
                // Vibrate once when Qibla is found
                if (!hasVibrated) {
                    vibrateDevice()
                    hasVibrated = true
                }
            } else {
                // Reset outer ring and triangle to original teal color
                binding.ivOuterRing.setColorFilter(android.graphics.Color.parseColor("#00897B"))
                binding.ivTriangle.setColorFilter(android.graphics.Color.parseColor("#00897B"))
                hasVibrated = false // Reset so it vibrates again when found
            }
        } else {
            // Show phone's current compass direction (azimuth) before location is obtained
            val phoneAngle = currentAzimuth.toInt()
            binding.tvAngle.text = String.format("%d°", phoneAngle)
            binding.tvRotationGuidance.text = "Getting location..."
        }
    }
    
    private fun vibrateDevice() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = requireContext().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(200)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }
    
    private fun checkLocationPermission() {
        Log.d(TAG, "Checking location permission")
        
        val fineLocation =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permission already granted")
            startLocationTracking()
        } else {
            Log.d(TAG, "Requesting permission")
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
        
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        // Create location request for continuous updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (_binding == null) return
                
                locationResult.lastLocation?.let { location ->
                    updateLocation(location)
                }
            }
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            requireActivity().mainLooper
        )
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
    }
    
    private fun updateLocation(location: Location) {
        if (_binding == null) return
        
        Log.d(TAG, "Location received: ${location.latitude}, ${location.longitude}")
        userLatitude = location.latitude
        userLongitude = location.longitude
        hasLocation = true
        
        // Update coordinates display continuously
        binding.tvCoordinates.text = String.format(
            "%.7f, %.7f",
            userLatitude,
            userLongitude
        )
        
        // Calculate Qibla direction and distance
        calculateQibla()
        
        // Update map markers
        updateMapMarkers()
    }
    
    private fun calculateQibla() {
        if (_binding == null) return
        
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
        
        // Update distance display
        binding.tvDistance.text = String.format("%.1fKM", distance)
        
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
    
    private fun showLocationBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_location, null)
        
        val rvLocations = bottomSheetView.findViewById<RecyclerView>(R.id.rv_locations)
        val btnChoose = bottomSheetView.findViewById<CardView>(R.id.btn_choose)
        val btnUseMyLocation = bottomSheetView.findViewById<View>(R.id.btn_use_my_location)
        val etSearch = bottomSheetView.findViewById<EditText>(R.id.et_search_location)
        val skeletonContainer = bottomSheetView.findViewById<View>(R.id.skeleton_container)
        
        // Reset selection
        selectedLocationIndex = -1
        
        // Setup RecyclerView with empty list initially
        rvLocations.layoutManager = LinearLayoutManager(requireContext())
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
        
        // Choose button click with animation
        btnChoose.setOnClickWithAnimation {
            if (selectedLocationIndex >= 0 && selectedLocationIndex < adapter.locations.size) {
                val selectedLocation = adapter.locations[selectedLocationIndex]
                
                // Stop any active GPS location tracking
                stopLocationUpdates()
                
                userLatitude = selectedLocation.latitude
                userLongitude = selectedLocation.longitude
                hasLocation = true
                
                binding.tvCoordinates.text = String.format("%.7f, %.7f", userLatitude, userLongitude)
                calculateQibla()
                updateMapMarkers()
                
                Toast.makeText(requireContext(), "Location set to ${selectedLocation.name}", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Use My Location click with animation
        btnUseMyLocation.setOnClickWithAnimation {
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
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
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
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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
                holder.locationCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background_light_gray))
            } else {
                holder.ivSelected.visibility = View.GONE
                holder.locationCard.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white))
            }
            
            holder.itemView.setOnClickWithAnimation {
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

