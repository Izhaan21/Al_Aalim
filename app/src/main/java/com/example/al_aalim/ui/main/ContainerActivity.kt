package com.example.al_aalim.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.al_aalim.R
import com.example.al_aalim.models.ChatConversation
import com.example.al_aalim.ui.auth.WelcomeActivity
import com.example.al_aalim.ui.features.QiblaScreen
import com.example.al_aalim.ui.features.QuranActivity
import com.example.al_aalim.ui.features.QuranScreen
import com.example.al_aalim.ui.features.StoreScreen
import com.example.al_aalim.ui.features.SurahReaderActivity
import com.example.al_aalim.ui.settings.ProfilePhotoActivity
import com.example.al_aalim.ui.settings.SettingsActivity
import com.example.al_aalim.ui.theme.*
import com.example.al_aalim.utils.LanguageManager
import com.example.al_aalim.utils.LocationManager as CustomLocationManager
import com.example.al_aalim.viewmodel.AccountViewModel
import com.example.al_aalim.viewmodel.AuthViewModel
import com.example.al_aalim.viewmodel.ChatHistoryState
import com.example.al_aalim.viewmodel.MainViewModel
import com.example.al_aalim.viewmodel.QuranViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import kotlin.math.*

class ContainerActivity : ComponentActivity(), SensorEventListener {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var accountViewModel: AccountViewModel
    private lateinit var quranViewModel: QuranViewModel

    // Qibla sensor fields
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

    // Qibla compose states
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

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) checkAndStartLocation()
        else {
            isLoadingLocationState.value = false
            showPermissionDeniedDialog.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val factory = ViewModelFactory(this)
        mainViewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
        accountViewModel = ViewModelProvider(this, factory)[AccountViewModel::class.java]
        quranViewModel = ViewModelProvider(this)[QuranViewModel::class.java]
        val authViewModel = ViewModelProvider(this, factory)[AuthViewModel::class.java]

        if (!authViewModel.isUserLoggedIn) {
             startActivity(Intent(this, WelcomeActivity::class.java))
             finish()
             return
         }

        // Qibla sensor setup
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

        com.example.al_aalim.utils.LocationManager.initialize(this)
        mainViewModel.loadConversations()
        accountViewModel.loadProfile()

        setContent {
            AlAalimTheme {
                ContainerScreen(
                    mainViewModel = mainViewModel,
                    accountViewModel = accountViewModel,
                    quranViewModel = quranViewModel,
                    // Qibla state
                    azimuth = currentAzimuthState.floatValue,
                    qiblaBearing = qiblaBearingState.floatValue,
                    isPhoneFlat = isPhoneFlatState.value,
                    hasQiblaLocation = hasLocationState.value,
                    isLoadingLocation = isLoadingLocationState.value,
                    qiblaDistance = distanceState.doubleValue,
                    qiblaLatitude = userLatitudeState.doubleValue,
                    qiblaLongitude = userLongitudeState.doubleValue,
                    showPermissionDeniedDialog = showPermissionDeniedDialog.value,
                    showGpsDialog = showGpsDialog.value,
                    onQiblaLocationRequest = { checkLocationPermission() },
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
                    onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                    onOpenAccount = {
                        val intent = Intent(this, SettingsActivity::class.java).apply {
                            putExtra("start_destination", com.example.al_aalim.ui.settings.SettingsRoute.Account.route)
                        }
                        startActivity(intent)
                    },
                    onOpenProfile = { startActivity(Intent(this, ProfilePhotoActivity::class.java)) },
                    onSurahClick = { surah ->
                        val intent = Intent(this, SurahReaderActivity::class.java).apply {
                            putExtra(SurahReaderActivity.EXTRA_SURAH_NUMBER, surah.number)
                            putExtra(SurahReaderActivity.EXTRA_SURAH_NAME, surah.englishName)
                            putExtra(SurahReaderActivity.EXTRA_SURAH_MEANING, surah.meaning)
                        }
                        startActivity(intent)
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
        if (::accountViewModel.isInitialized) {
            accountViewModel.loadProfile()
        }
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isLoadingLocationState.value = false
            return
        }
        // Try cached last-known location first — returns instantly
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLatitudeState.doubleValue = location.latitude
                    userLongitudeState.doubleValue = location.longitude
                    hasLocationState.value = true
                    isLoadingLocationState.value = false
                    calculateQibla()
                } else {
                    // No cached fix — request a fresh one
                    requestFreshLocation()
                }
            }
            .addOnFailureListener {
                requestFreshLocation()
            }
    }

    private fun requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            isLoadingLocationState.value = false
            return
        }
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    userLatitudeState.doubleValue = location.latitude
                    userLongitudeState.doubleValue = location.longitude
                    hasLocationState.value = true
                    isLoadingLocationState.value = false
                    calculateQibla()
                } else {
                    isLoadingLocationState.value = false
                }
            }
            .addOnFailureListener {
                isLoadingLocationState.value = false
            }
    }

    private fun calculateQibla() {
        val dLon = Math.toRadians(MECCA_LONGITUDE - userLongitudeState.doubleValue)
        val lat1 = Math.toRadians(userLatitudeState.doubleValue)
        val lat2 = Math.toRadians(MECCA_LATITUDE)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        qiblaBearingState.floatValue = ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()

        val dLat = Math.toRadians(MECCA_LATITUDE - userLatitudeState.doubleValue)
        val dLon2 = Math.toRadians(MECCA_LONGITUDE - userLongitudeState.doubleValue)
        val a = sin(dLat / 2) * sin(dLat / 2) + cos(lat1) * cos(lat2) * sin(dLon2 / 2) * sin(dLon2 / 2)
        distanceState.doubleValue = 6371.0 * (2 * atan2(sqrt(a), sqrt(1 - a)))
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}

@Composable
fun ContainerScreen(
    mainViewModel: MainViewModel,
    accountViewModel: AccountViewModel,
    quranViewModel: QuranViewModel,
    // Qibla params
    azimuth: Float,
    qiblaBearing: Float,
    isPhoneFlat: Boolean,
    hasQiblaLocation: Boolean,
    isLoadingLocation: Boolean,
    qiblaDistance: Double,
    qiblaLatitude: Double,
    qiblaLongitude: Double,
    showPermissionDeniedDialog: Boolean,
    showGpsDialog: Boolean,
    onQiblaLocationRequest: () -> Unit,
    onDismissPermissionDialog: () -> Unit,
    onDismissGpsDialog: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenProfile: () -> Unit,
    onSurahClick: (com.example.al_aalim.model.Surah) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0) { 4 }

    // Hoist HomeViewModel here so onNewChat can reset it instantly — no LaunchedEffect delay
    val homeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.al_aalim.viewmodel.HomeViewModel>(
        factory = ViewModelFactory(LocalContext.current)
    )

    // Keep homeViewModel in sync whenever the active conversation changes
    val activeId by mainViewModel.activeConversationId.collectAsStateWithLifecycle()
    LaunchedEffect(activeId) {
        homeViewModel.setActiveConversation(activeId)
    }

    // Sync bottom nav with pager
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentPage == 0, // Only allow drawer gesture on Home
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = DrawerBackgroundTeal,
                modifier = Modifier.width(300.dp)
            ) {
                ChatDrawerContent(
                    mainViewModel = mainViewModel,
                    accountViewModel = accountViewModel,
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onNewChat = {
                        // Reset both VMs immediately — UI responds before drawer even closes
                        mainViewModel.setActiveConversation(null)
                        homeViewModel.setActiveConversation(null)
                        scope.launch {
                            pagerState.scrollToPage(0) // Snap to home page instantly
                            drawerState.close()
                        }
                    },
                    onOpenAccount = onOpenAccount,
                    onOpenSettings = onOpenSettings,
                    onOpenProfile = onOpenProfile
                )
            }
        }
    ) {
        val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

        Scaffold(
            bottomBar = {
                if (!isKeyboardVisible) {
                    BottomNavBar(
                        selectedTab = currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                    )
                }
            },
            containerColor = BackgroundGradientEnd
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = paddingValues.calculateBottomPadding())
                    .consumeWindowInsets(PaddingValues(bottom = paddingValues.calculateBottomPadding())),
                beyondViewportPageCount = 1, // Pre-compose adjacent pages
                userScrollEnabled = true
            ) { page ->
                when (page) {
                    0 -> {
                        // Home / Chat — uses hoisted homeViewModel, no LaunchedEffect bridge needed
                        HomeRoute(
                            viewModel = homeViewModel,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onAccountClick = onOpenAccount,
                            onRefreshDrawer = { mainViewModel.loadConversations() }
                        )
                    }
                    1 -> {
                        // Qibla (embedded)
                        QiblaScreen(
                            azimuth = azimuth,
                            qiblaBearing = qiblaBearing,
                            isPhoneFlat = isPhoneFlat,
                            hasLocation = hasQiblaLocation,
                            isLoadingLocation = isLoadingLocation,
                            distance = qiblaDistance,
                            latitude = qiblaLatitude,
                            longitude = qiblaLongitude,
                            showPermissionDeniedDialog = showPermissionDeniedDialog,
                            showGpsDialog = showGpsDialog,
                            onLocationRequest = onQiblaLocationRequest,
                            onDismissPermissionDialog = onDismissPermissionDialog,
                            onDismissGpsDialog = onDismissGpsDialog,
                            onOpenAppSettings = onOpenAppSettings,
                            onOpenLocationSettings = onOpenLocationSettings,
                            onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                            onNavigate = { tab -> scope.launch { pagerState.animateScrollToPage(tab) } },
                            showBottomNav = false
                        )
                    }
                    2 -> {
                        // Quran (embedded)
                        QuranScreen(
                            viewModel = quranViewModel,
                            onBack = { scope.launch { pagerState.animateScrollToPage(0) } },
                            onNavigate = { tab -> scope.launch { pagerState.animateScrollToPage(tab) } },
                            onSurahClick = onSurahClick,
                            showBottomNav = false
                        )
                    }
                    3 -> {
                        // Store (embedded)
                        StoreScreen(
                            onBackClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            showBottomNav = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDrawerContent(
    mainViewModel: MainViewModel,
    accountViewModel: AccountViewModel,
    onCloseDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    val profileState by accountViewModel.profileState.collectAsStateWithLifecycle()
    val chatHistoryState by mainViewModel.chatHistoryState.collectAsStateWithLifecycle()
    val activeConversationId by mainViewModel.activeConversationId.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    
    // Filter chats based on search
    val conversations = remember(chatHistoryState, searchQuery) {
        if (chatHistoryState is ChatHistoryState.Success) {
            val list = (chatHistoryState as ChatHistoryState.Success).conversations
            if (searchQuery.isNotEmpty()) {
                list.filter { it.title.contains(searchQuery, ignoreCase = true) }
            } else {
                list
            }
        } else {
            emptyList<ChatConversation>()
        }
    }

    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        com.example.al_aalim.ui.components.ConfirmationDialog(
            title = "Delete All Chats",
            message = "Are you sure you want to delete all ${conversations.size} conversations? This cannot be undone.",
            confirmText = "Delete All",
            cancelText = "Cancel",
            onConfirm = {
                mainViewModel.deleteAllConversations { success ->
                    if (success) onCloseDrawer()
                }
            },
            onDismiss = { showDeleteAllDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- Header Section (Profile) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 32.dp, 16.dp, 16.dp)
                .clickable { onOpenAccount() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(
                        width = 2.dp,
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep)),
                        shape = CircleShape
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .clickable { onOpenProfile() },
                contentAlignment = Alignment.Center
            ) {
                if (profileState.profileImage != null) {
                    androidx.compose.foundation.Image(
                        bitmap = profileState.profileImage!!.asImageBitmap(),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Gold, com.example.al_aalim.ui.theme.GoldDeep))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profileState.initials,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Salam,", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                Text(
                    text = profileState.name.ifEmpty { "User" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        // --- Action Buttons ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))))
                .clickable { onNewChat() },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Search Bar ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search chats...", color = Color.White.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryTeal,
                unfocusedBorderColor = SelectedItemTeal,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Chat History List ---
        Text(
            text = "Chat History",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(conversations) { conversation ->
                var expanded by remember { mutableStateOf(false) }
                var showRenameDialog by remember { mutableStateOf(false) }
                var newTitle by remember { mutableStateOf(conversation.title) }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (activeConversationId == conversation.id) SelectedItemTeal else Color.Transparent)
                        .clickable {
                            mainViewModel.setActiveConversation(conversation.id)
                            onCloseDrawer()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = conversation.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            fontWeight = if (activeConversationId == conversation.id) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(conversation.updatedAt)),
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(BackgroundGradientStart)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename", color = Color.White) },
                                onClick = {
                                    expanded = false
                                    newTitle = conversation.title
                                    showRenameDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = {
                                    expanded = false
                                    mainViewModel.deleteConversation(conversation.id) { _ -> 
                                        if (activeConversationId == conversation.id) {
                                            mainViewModel.setActiveConversation(null)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                
                if (showRenameDialog) {
                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Rename Conversation") },
                        text = {
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (newTitle.isNotBlank()) {
                                    mainViewModel.renameConversation(conversation.id, newTitle.trim()) { _ -> }
                                }
                                showRenameDialog = false
                            }) {
                                Text("Save", color = GoldGradientStart)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRenameDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                        },
                        containerColor = DrawerBackgroundTeal,
                        titleContentColor = Color.White,
                        textContentColor = Color.White
                    )
                }
            }
        }

        if (conversations.isNotEmpty()) {
            Text(
                text = "Delete All",
                color = Color.Red,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteAllDialog = true }
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 14.sp
            )
        }

        // --- Bottom Actions ---
        HorizontalDivider(color = SelectedItemTeal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSettings() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_settings), contentDescription = "Settings", tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Settings", color = Color.White, fontSize = 16.sp)
        }
    }
}
