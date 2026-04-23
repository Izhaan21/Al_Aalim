package com.example.al_aalim.ui.features

import com.example.al_aalim.R
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
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.animation.core.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.al_aalim.ui.components.ConfirmationDialog
import com.example.al_aalim.ui.theme.GoldGradientStart
import com.example.al_aalim.ui.theme.GoldGradientEnd
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapProperties

import com.example.al_aalim.data.CountriesData
import com.example.al_aalim.data.CountryLocation
import com.example.al_aalim.ui.main.BottomNavBar
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.ui.theme.BrandYellow
import com.example.al_aalim.ui.theme.BackgroundGradientStart
import com.example.al_aalim.ui.theme.BackgroundGradientEnd
import com.example.al_aalim.ui.theme.SecondaryTeal
import com.example.al_aalim.ui.theme.GradientTeal
import com.example.al_aalim.utils.LanguageManager
import com.example.al_aalim.utils.LocationManager as CustomLocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

class QiblaActivity : ComponentActivity(), SensorEventListener {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager

    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)

    private val FLAT_THRESHOLD = 35

    private val MECCA_LATITUDE = 21.4225
    private val MECCA_LONGITUDE = 39.8262

    // Compose States
    private val currentAzimuthState = mutableFloatStateOf(0f)
    private val qiblaBearingState = mutableFloatStateOf(0f)
    private val isPhoneFlatState = mutableStateOf(false)
    private val hasLocationState = mutableStateOf(false)
    private val distanceState = mutableDoubleStateOf(0.0)
    private val userLatitudeState = mutableDoubleStateOf(0.0)
    private val userLongitudeState = mutableDoubleStateOf(0.0)
    private val isLoadingLocationState = mutableStateOf(false)
    
    private val showPermissionDeniedDialog = mutableStateOf(false)
    private val showGpsDialog = mutableStateOf(false)

    companion object {
        private const val TAG = "QiblaActivity"
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            checkAndStartLocation()
        } else {
            isLoadingLocationState.value = false
            showPermissionDeniedDialog.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (CustomLocationManager.hasLocation) {
            userLatitudeState.doubleValue = CustomLocationManager.userLatitude
            userLongitudeState.doubleValue = CustomLocationManager.userLongitude
            hasLocationState.value = true
            calculateQibla()
        }

        setContent {
            AlAalimTheme {
                QiblaScreen(
                    azimuth = currentAzimuthState.floatValue,
                    qiblaBearing = qiblaBearingState.floatValue,
                    isPhoneFlat = isPhoneFlatState.value,
                    hasLocation = hasLocationState.value,
                    isLoadingLocation = isLoadingLocationState.value,
                    distance = distanceState.doubleValue,
                    latitude = userLatitudeState.doubleValue,
                    longitude = userLongitudeState.doubleValue,
                    showPermissionDeniedDialog = showPermissionDeniedDialog.value,
                    showGpsDialog = showGpsDialog.value,
                    onLocationRequest = { checkLocationPermission() },
                    onDismissPermissionDialog = { showPermissionDeniedDialog.value = false },
                    onDismissGpsDialog = { showGpsDialog.value = false },
                    onOpenAppSettings = {
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    },
                    onOpenLocationSettings = {
                        val intent = Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivity(intent)
                    },
                    onBack = { finish() },
                    onNavigate = { tab ->
                        when(tab) {
                            0 -> finish()
                            1 -> { /* already here */ }
                            2 -> { startActivity(Intent(this@QiblaActivity, QuranActivity::class.java)); finish() }
                            3 -> { startActivity(Intent(this@QiblaActivity, StoreActivity::class.java)); finish() }
                        }
                    }
                )
            }
        }
    }

    private val gpsReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == android.location.LocationManager.PROVIDERS_CHANGED_ACTION) {
                val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
                val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || 
                                   locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
                if (!isGpsEnabled) {
                    hasLocationState.value = false
                    userLatitudeState.doubleValue = 0.0
                    userLongitudeState.doubleValue = 0.0
                    distanceState.doubleValue = 0.0
                } else {
                    checkLocationPermission()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        registerReceiver(gpsReceiver, android.content.IntentFilter(android.location.LocationManager.PROVIDERS_CHANGED_ACTION))
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        unregisterReceiver(gpsReceiver)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val alpha = 0.97f
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]
            } else if (it.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                geomagnetic[0] = alpha * geomagnetic[0] + (1 - alpha) * it.values[0]
                geomagnetic[1] = alpha * geomagnetic[1] + (1 - alpha) * it.values[1]
                geomagnetic[2] = alpha * geomagnetic[2] + (1 - alpha) * it.values[2]
            }

            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                SensorManager.getOrientation(rotationMatrix, orientation)
                var azimuthInDegrees = Math.toDegrees(orientation[0].toDouble()).toFloat()
                azimuthInDegrees = (azimuthInDegrees + 360) % 360
                
                currentAzimuthState.floatValue = azimuthInDegrees

                val pitch = Math.toDegrees(atan2(gravity[1].toDouble(), gravity[2].toDouble())).toFloat()
                val roll = Math.toDegrees(atan2(gravity[0].toDouble(), gravity[2].toDouble())).toFloat()
                isPhoneFlatState.value = abs(pitch) < FLAT_THRESHOLD && abs(roll) < FLAT_THRESHOLD
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun checkLocationPermission() {
        isLoadingLocationState.value = true
        val fineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED) {
            checkAndStartLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun checkAndStartLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        if (!isGpsEnabled) {
            isLoadingLocationState.value = false
            showGpsDialog.value = true
        } else {
            startLocationTracking()
        }
    }

    private fun startLocationTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).build()
            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        userLatitudeState.doubleValue = location.latitude
                        userLongitudeState.doubleValue = location.longitude
                        hasLocationState.value = true
                        isLoadingLocationState.value = false
                        calculateQibla()
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, android.os.Looper.getMainLooper())
        } else {
            isLoadingLocationState.value = false
        }
    }

    private fun calculateQibla() {
        qiblaBearingState.floatValue = calculateBearing(
            userLatitudeState.doubleValue, userLongitudeState.doubleValue,
            MECCA_LATITUDE, MECCA_LONGITUDE
        ).toFloat()
        
        distanceState.doubleValue = calculateDistance(
            userLatitudeState.doubleValue, userLongitudeState.doubleValue,
            MECCA_LATITUDE, MECCA_LONGITUDE
        )
    }

    private fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val y = sin(dLon) * cos(lat2Rad)
        val x = cos(lat1Rad) * sin(lat2Rad) - sin(lat1Rad) * cos(lat2Rad) * cos(dLon)
        var bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360) % 360
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 6371.0 * (2 * atan2(sqrt(a), sqrt(1 - a)))
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    azimuth: Float,
    qiblaBearing: Float,
    isPhoneFlat: Boolean,
    hasLocation: Boolean,
    isLoadingLocation: Boolean,
    distance: Double,
    latitude: Double,
    longitude: Double,
    showPermissionDeniedDialog: Boolean,
    showGpsDialog: Boolean,
    onLocationRequest: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onDismissGpsDialog: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit,
    showBottomNav: Boolean = true
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var angleDiff = qiblaBearing - azimuth
    while (angleDiff > 180) angleDiff -= 360
    while (angleDiff < -180) angleDiff += 360
    val displayAngle = abs(angleDiff).toInt()

    val rotationGuidance = if (!hasLocation) {
        if (isLoadingLocation) "Locating..." else "Tap 'Use My Location' to find Qibla"
    } else {
        when {
            displayAngle <= 2 -> "Qibla found! ✓"
            displayAngle <= 5 -> "Almost there!"
            angleDiff > 0 -> "Rotate ${displayAngle}° to the right"
            else -> "Rotate ${displayAngle}° to the left"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundGradientStart, BackgroundGradientEnd)))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showBottomNav) 65.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Header (Home Style) ---
            // --- Header (Home Style) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .border(1.dp, BrandYellow, RoundedCornerShape(30.dp))
                        .background(Color(0xFF0D4A4C))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                ) {
                    Text("Qibla", color = BrandYellow, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Box(modifier = Modifier.size(44.dp)) // Removes Profile Icon and balances the row
            }

            // Map Card
            val locationLatLng = LatLng(
                if (latitude != 0.0) latitude else 21.4225, 
                if (longitude != 0.0) longitude else 39.8262
            )
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(locationLatLng, 4f)
            }

            // Re-center and zoom to show both user and Kaaba when location updates
            LaunchedEffect(latitude, longitude) {
                if (latitude != 0.0 && longitude != 0.0) {
                    val userPath = com.google.android.gms.maps.model.LatLng(latitude, longitude)
                    val meccaPath = com.google.android.gms.maps.model.LatLng(21.4225, 39.8262)
                    
                    val bounds = com.google.android.gms.maps.model.LatLngBounds.builder()
                        .include(userPath)
                        .include(meccaPath)
                        .build()

                    cameraPositionState.animate(
                        update = com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 180),
                        durationMs = 1500
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            compassEnabled = false,
                            myLocationButtonEnabled = false,
                            scrollGesturesEnabled = true
                        )
                    ) {
                        // Plot Mecca Marker
                        val kaabaIcon = remember(context) {
                            val vectorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_kaaba)
                            if (vectorDrawable != null) {
                                vectorDrawable.setBounds(0, 0, (vectorDrawable.intrinsicWidth * 0.8).toInt(), (vectorDrawable.intrinsicHeight * 0.8).toInt())
                                val bitmap = android.graphics.Bitmap.createBitmap((vectorDrawable.intrinsicWidth * 0.8).toInt(), (vectorDrawable.intrinsicHeight * 0.8).toInt(), android.graphics.Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                vectorDrawable.draw(canvas)
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap)
                            } else null
                        }

                        Marker(
                            state = MarkerState(position = LatLng(21.4225, 39.8262)),
                            title = "Mecca",
                            snippet = "Kaaba",
                            icon = kaabaIcon
                        )
                        // User location marker and route to Kaaba
                        if (latitude != 0.0 && longitude != 0.0) {
                            Marker(
                                state = MarkerState(position = LatLng(latitude, longitude)),
                                title = "Your Location"
                            )
                            
                            // Golden route linking user directly to Kaaba
                            com.google.maps.android.compose.Polyline(
                                points = listOf(
                                    LatLng(latitude, longitude),
                                    LatLng(21.4225, 39.8262)
                                ),
                                color = BrandYellow,
                                width = 5f,
                                geodesic = true
                            )
                        }
                    }
                }
            }

            // Info Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(String.format("%.1fKM", distance), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Distance from Mecca", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                
                Column(modifier = Modifier.weight(1f).padding(start = 24.dp)) {
                    Text("${displayAngle}°", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Device's Angle to Qibla", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
            }

            // Compass View
            val haptic = LocalHapticFeedback.current
            val isQiblaFound = displayAngle <= 2
            
            LaunchedEffect(isQiblaFound) {
                if (isQiblaFound) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            val infiniteTransition = rememberInfiniteTransition()
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "PulseAlpha"
            )

            Box(
                modifier = Modifier.size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isQiblaFound) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .background(com.example.al_aalim.ui.theme.GoldGradientStart.copy(alpha = pulseAlpha))
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val centerOut = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                    val radiusOut = size.width / 2f

                    // 0) Inner glass tint (Fills the entire compass so the needle stands out)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x33FFFFFF), Color(0x05FFFFFF)),
                            center = centerOut,
                            radius = radiusOut
                        ),
                        radius = radiusOut - 20.dp.toPx(),
                        center = centerOut
                    )

                    // 1) Glowing Outer Golden Ring (Stroke)
                    drawCircle(
                        brush = Brush.linearGradient(listOf(GoldGradientStart, GoldGradientEnd)),
                        radius = radiusOut - 20.dp.toPx(),
                        center = centerOut,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                    )

                    // 2) Inner Dashed Ring (Teal)
                    drawCircle(
                        color = Color(0xFF5AB4B4).copy(alpha = 0.5f),
                        radius = radiusOut - 35.dp.toPx(),
                        center = centerOut,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )

                    // 3) Tick Marks around the compass
                    val tickRadius = radiusOut - 20.dp.toPx()
                    for (i in 0 until 360 step 5) {
                        val angleRad = Math.toRadians(i.toDouble() - 90.0) // Start from top
                        val isPrimary = i % 90 == 0
                        val isSecondary = i % 30 == 0
                        
                        val tickLength = when {
                            isPrimary -> 14.dp.toPx()
                            isSecondary -> 8.dp.toPx()
                            else -> 4.dp.toPx()
                        }
                        
                        val strokeWidth = if (isPrimary) 2.dp.toPx() else 1.dp.toPx()
                        val tickColor = if (isPrimary) BrandYellow else Color.White.copy(alpha = 0.4f)
                        
                        val startX = centerOut.x + (tickRadius - tickLength) * cos(angleRad).toFloat()
                        val startY = centerOut.y + (tickRadius - tickLength) * sin(angleRad).toFloat()
                        
                        val stopX = centerOut.x + tickRadius * cos(angleRad).toFloat()
                        val stopY = centerOut.y + tickRadius * sin(angleRad).toFloat()
                        
                        drawLine(
                            color = tickColor,
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(stopX, stopY),
                            strokeWidth = strokeWidth
                        )
                    }

                    // Top pointing triangle attached to outer border pointing inwards
                    val trianglePath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(centerOut.x - 14.dp.toPx(), 20.dp.toPx()) // Base left
                        lineTo(centerOut.x + 14.dp.toPx(), 20.dp.toPx()) // Base right
                        lineTo(centerOut.x, 38.dp.toPx())              // Point downwards
                        close()
                    }
                    drawPath(trianglePath, brush = Brush.linearGradient(listOf(GoldGradientStart, BrandYellow)))
                }

                // Directional Symbols rotating to maintain true North
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp)
                        .rotate(-azimuth),
                ) {
                    Text(
                        text = "N", 
                        color = BrandYellow, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 20.sp, 
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    Text(
                        text = "S", 
                        color = Color.White.copy(alpha = 0.7f), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    Text(
                        text = "E", 
                        color = Color.White.copy(alpha = 0.7f), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                    Text(
                        text = "W", 
                        color = Color.White.copy(alpha = 0.7f), 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 16.sp, 
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                // Kaaba marker pointing towards Qibla
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(qiblaBearing - azimuth),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kaaba),
                        contentDescription = "Kaaba",
                        modifier = Modifier
                            .offset(y = (-105).dp)
                            .size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(rotationGuidance, color = Color.White, fontSize = 15.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(id = R.drawable.ic_location_pin_filled), "Location", tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(String.format("%.4f° N, %.4f° E", latitude, longitude), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            Text("May Allah accept your prayers", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            
            // Text is transparent instead of entirely removed so layout sizing NEVER changes
            Text(
                text = "Please lay your phone flat", 
                color = Color.White.copy(alpha = if (!isPhoneFlat) 0.6f else 0f), 
                fontSize = 12.sp, 
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            if (!hasLocation) {
                if (isLoadingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp).size(48.dp),
                        color = BrandYellow,
                        strokeWidth = 3.dp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.linearGradient(listOf(com.example.al_aalim.ui.theme.Gold, Color(0xFFC59A45))))
                            .clickable { onLocationRequest() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Use My Location", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }

        if (showBottomNav) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                BottomNavBar(selectedTab = 1, onTabSelected = onNavigate)
            }
        }
    }

    if (showPermissionDeniedDialog) {
        ConfirmationDialog(
            title = "Permission Denied",
            message = "Location permission is required to find the Qibla direction. Please enable it in the App Settings.",
            confirmText = "Open Settings",
            cancelText = "Cancel",
            onConfirm = { onOpenAppSettings() },
            onDismiss = { onDismissPermissionDialog() }
        )
    }

    if (showGpsDialog) {
        ConfirmationDialog(
            title = "Location Services Disabled",
            message = "Please turn on Location Services (GPS) to find your Qibla direction.",
            confirmText = "Turn On",
            cancelText = "Cancel",
            onConfirm = { onOpenLocationSettings() },
            onDismiss = { onDismissGpsDialog() }
        )
    }
}
