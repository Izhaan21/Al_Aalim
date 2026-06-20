package com.example.al_aalim.ui.permissions

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
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.al_aalim.R
import com.example.al_aalim.ui.components.PermissionDialog
import com.example.al_aalim.ui.settings.LanguageSelectionActivity
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.utils.LocationManager
import com.example.al_aalim.viewmodel.SettingsViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory

class LocationPermissionActivity : ComponentActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var locationListener: ((Double, Double) -> Unit)? = null
    private val LOCATION_TIMEOUT_MS = 10000L
    private var hasRequestedPermission = false
    private var wentToLocationSettings = false
    private var locationTimeoutRunnable: Runnable? = null

    // Compose state hoisted to Activity level
    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: ((Boolean) -> Unit)? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                saveLocationPermissionGranted()
                onPermissionGranted?.invoke()
            }
            else -> {
                val isPermanentlyDenied = hasRequestedPermission &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_FINE_LOCATION
                    )
                onPermissionDenied?.invoke(isPermanentlyDenied)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Avoid calling Window.getInsetsController too early (can be null on some devices/OS builds)
        window.decorView.post { hideSystemBars() }

        setContent {
            AlAalimTheme(darkTheme = false) {
                LocationPermissionScreen(
                    onRequestPermission = { granted, denied ->
                        onPermissionGranted = granted
                        onPermissionDenied = denied
                        requestLocationPermission()
                    },
                    onStartLocationFetch = { onFound, onTimeout ->
                        startLocationFetchWithProgress(onFound, onTimeout)
                    },
                    onCancelFetch = {
                        cancelPendingLocationWork()
                        LocationManager.stopLocationUpdates()
                        locationListener?.let { LocationManager.removeLocationListener(it) }
                    },
                    onNavigateToMain = { navigateToMain() },
                    onOpenAppSettings = { openAppSettings() },
                    onOpenLocationSettings = {
                        wentToLocationSettings = true
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    },
                    isLocationEnabled = { isLocationEnabled() },
                )
            }
        }
    }

    private fun requestLocationPermission() {
        if (!isLocationEnabled()) {
            // LocationPermissionScreen shows location-services-disabled dialog via state
            return
        }
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED -> {
                saveLocationPermissionGranted()
                onPermissionGranted?.invoke()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                onPermissionDenied?.invoke(false) // show rationale dialog
            }
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
        val lm = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
               lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
    }

    private fun startLocationFetchWithProgress(onFound: (Boolean) -> Unit, onTimeout: () -> Unit) {
        LocationManager.initialize(this)
        LocationManager.startLocationUpdates(this)

        locationListener = { _, _ ->
            locationListener?.let { LocationManager.removeLocationListener(it) }
            LocationManager.stopLocationUpdates()
            cancelPendingLocationWork()
            handler.post { onFound(true) }
        }
        LocationManager.addLocationListener(locationListener!!)

        val timeoutRunnable = Runnable {
            locationListener?.let { LocationManager.removeLocationListener(it) }
            LocationManager.stopLocationUpdates()
            handler.post { onTimeout() }
        }
        locationTimeoutRunnable = timeoutRunnable
        handler.postDelayed(timeoutRunnable, LOCATION_TIMEOUT_MS)
    }

    private fun cancelPendingLocationWork() {
        locationTimeoutRunnable?.let { handler.removeCallbacks(it) }
        locationTimeoutRunnable = null
    }

    private fun navigateToMain() {
        if (intent.getBooleanExtra("from_settings", false)) { finish(); return }
        val factory = ViewModelFactory(this)
        ViewModelProvider(this, factory)[SettingsViewModel::class.java].setOnboardingCompleted(true)
        val fromAuth = intent.getBooleanExtra("from_auth", false)
        val nextIntent = Intent(this, NotificationPermissionActivity::class.java)
        if (fromAuth) {
            nextIntent.putExtra("from_auth", true)
            // Don't clear task — just move forward in the onboarding chain
        } else {
            nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(nextIntent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun saveLocationPermissionGranted() {
        val factory = ViewModelFactory(this)
        ViewModelProvider(this, factory)[SettingsViewModel::class.java].setLocationPermissionGranted(true)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (wentToLocationSettings && isLocationEnabled()) {
            wentToLocationSettings = false
            hasRequestedPermission = true
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelPendingLocationWork()
        locationListener?.let { LocationManager.removeLocationListener(it) }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
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
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.al_aalim.utils.LanguageManager.applyLanguage(newBase))
    }
}

// ─────────────────────────────────────────────
// Permission dialog state
// ─────────────────────────────────────────────

private enum class PermissionDialogType {
    NONE,
    LOCATION_SERVICES_DISABLED,
    PERMISSION_DENIED,
    PERMANENTLY_DENIED,
    RATIONALE,
}

private enum class LoadingState { IDLE, LOADING, FOUND, TIMEOUT }

// ─────────────────────────────────────────────
// Compose UI
// ─────────────────────────────────────────────

@Composable
private fun LocationPermissionScreen(
    onRequestPermission: (onGranted: () -> Unit, onDenied: (Boolean) -> Unit) -> Unit,
    onStartLocationFetch: (onFound: (Boolean) -> Unit, onTimeout: () -> Unit) -> Unit,
    onCancelFetch: () -> Unit,
    onNavigateToMain: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    isLocationEnabled: () -> Boolean,
) {
    var dialogType by remember { mutableStateOf(PermissionDialogType.NONE) }
    var loadingState by remember { mutableStateOf(LoadingState.IDLE) }
    var loadingText by remember { mutableStateOf("Getting your location...") }

    val onGranted: () -> Unit = remember {
        {
            loadingState = LoadingState.LOADING
            loadingText = "Getting your location..."
            onStartLocationFetch(
                { _ ->
                    loadingText = "Location found!"
                    loadingState = LoadingState.FOUND
                    Handler(Looper.getMainLooper()).postDelayed({ onNavigateToMain() }, 500)
                },
                {
                    loadingText = "Continuing..."
                    loadingState = LoadingState.TIMEOUT
                    Handler(Looper.getMainLooper()).postDelayed({ onNavigateToMain() }, 300)
                }
            )
        }
    }
    val onDenied: (Boolean) -> Unit = remember {
        { isPermanent ->
            dialogType = if (isPermanent) PermissionDialogType.PERMANENTLY_DENIED
                         else PermissionDialogType.PERMISSION_DENIED
        }
    }

    fun handleAllowClick() {
        if (!isLocationEnabled()) {
            dialogType = PermissionDialogType.LOCATION_SERVICES_DISABLED
            return
        }
        onRequestPermission(onGranted, onDenied)
    }

    // Entrance animations
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val contentAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 300), label = "contentA"
    )
    val contentOffset by animateFloatAsState(
        targetValue = if (visible) 0f else 80f,
        animationSpec = tween(800, delayMillis = 300, easing = FastOutSlowInEasing), label = "contentO"
    )
    val mosqueAlpha by animateFloatAsState(
        targetValue = if (visible) 0.35f else 0f,
        animationSpec = tween(1000, delayMillis = 500), label = "mosqueA"
    )

    // Pulsing Radar Animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radarScale1 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 3f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "rs1"
    )
    val radarAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart), label = "ra1"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF041A1B), Color(0xFF0A3D3F), Color(0xFF136A6C))
                )
            )
    ) {
        // Mosque silhouette at bottom

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp)
                .graphicsLayer { alpha = contentAlpha; translationY = contentOffset },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Animated Location Radar
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer Ripple
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .graphicsLayer { scaleX = radarScale1; scaleY = radarScale1; alpha = radarAlpha1 }
                        .background(Gold, CircleShape)
                )
                // Center Pin
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(12.dp, CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF136A6C), Color(0xFF041A1B))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings_location),
                        contentDescription = "Location icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            Text(
                text = "Discover Qibla &\nPrayer Times",
                color = Gold,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Allow location access to accurately determine the Qibla direction and local prayer times wherever you are.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.weight(0.8f))

            // Allow location button (Premium Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { handleAllowClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Allow Location",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Maybe Later",
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable { onNavigateToMain() }
                    .padding(8.dp)
            )
            
            Spacer(Modifier.height(8.dp))
        }

        // Loading overlay
        AnimatedVisibility(
            visible = loadingState == LoadingState.LOADING ||
                      loadingState == LoadingState.FOUND ||
                      loadingState == LoadingState.TIMEOUT,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A3D3F)),
                    elevation = CardDefaults.cardElevation(16.dp),
                    modifier = Modifier.padding(48.dp, 32.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (loadingState == LoadingState.LOADING) {
                            CircularProgressIndicator(color = Gold, modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Gold, modifier = Modifier.size(48.dp))
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = loadingText,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // Permission dialogs
        when (dialogType) {
            PermissionDialogType.LOCATION_SERVICES_DISABLED -> PermissionDialog(
                title = "📍 Enable Location Services",
                message = "Location services are turned off.\n\n" +
                    "To use Al-Aalim's Qibla and prayer times features:\n\n" +
                    "1. Tap 'Open Settings'\n2. Turn on 'Location'\n3. Return to Al-Aalim",
                iconRes = R.drawable.ic_location,
                confirmText = stringResource(R.string.open_settings),
                cancelText = stringResource(R.string.skip),
                onConfirm = { dialogType = PermissionDialogType.NONE; onOpenLocationSettings() },
                onCancel = { dialogType = PermissionDialogType.NONE; onNavigateToMain() },
                onDismiss = { dialogType = PermissionDialogType.NONE }
            )
            PermissionDialogType.PERMISSION_DENIED -> PermissionDialog(
                title = "📍 Location Access Required",
                message = "Location is essential for:\n\n🕋 Accurate Qibla Direction\n🕌 Prayer Times\n\n" +
                    "Without it, you'll need to manually select your location.",
                iconRes = R.drawable.ic_location,
                confirmText = stringResource(R.string.try_again),
                cancelText = stringResource(R.string.skip),
                onConfirm = {
                    dialogType = PermissionDialogType.NONE
                    onRequestPermission(onGranted, onDenied)
                },
                onCancel = { dialogType = PermissionDialogType.NONE; onNavigateToMain() },
                onDismiss = { dialogType = PermissionDialogType.NONE }
            )
            PermissionDialogType.PERMANENTLY_DENIED -> PermissionDialog(
                title = "📍 Permission Required",
                message = "You permanently denied location access.\n\n" +
                    "To enable:\n1. Tap 'Open Settings'\n2. Go to 'Permissions'\n3. Enable 'Location'",
                iconRes = R.drawable.ic_location,
                confirmText = stringResource(R.string.open_settings),
                cancelText = stringResource(R.string.skip),
                onConfirm = { dialogType = PermissionDialogType.NONE; onOpenAppSettings() },
                onCancel = { dialogType = PermissionDialogType.NONE; onNavigateToMain() },
                onDismiss = { dialogType = PermissionDialogType.NONE }
            )
            PermissionDialogType.RATIONALE -> PermissionDialog(
                title = "📍 Why We Need Location",
                message = "Al-Aalim uses your location to:\n\n" +
                    "• Show accurate Qibla direction\n• Calculate prayer times\n\n" +
                    "Your location stays on your device and is never shared.",
                iconRes = R.drawable.ic_location,
                confirmText = stringResource(R.string.allow_location),
                cancelText = "Not Now",
                onConfirm = { dialogType = PermissionDialogType.NONE; onRequestPermission(onGranted, onDenied) },
                onCancel = { dialogType = PermissionDialogType.NONE; onNavigateToMain() },
                onDismiss = { dialogType = PermissionDialogType.NONE }
            )
            PermissionDialogType.NONE -> {}
        }
    }
}

