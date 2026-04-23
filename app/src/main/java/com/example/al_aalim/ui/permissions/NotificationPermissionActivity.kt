package com.example.al_aalim.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.example.al_aalim.R
import com.example.al_aalim.ui.components.PermissionDialog
import com.example.al_aalim.ui.main.ContainerActivity
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.ui.theme.Gold
import com.example.al_aalim.viewmodel.SettingsViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory

class NotificationPermissionActivity : ComponentActivity() {

    private var onPermissionResult: ((Boolean) -> Unit)? = null

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult?.invoke(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.post { hideSystemBars() }

        setContent {
            AlAalimTheme(darkTheme = false) {
                NotificationPermissionScreen(
                    onRequestPermission = { onResult ->
                        onPermissionResult = onResult
                        requestNotificationPermission(onResult)
                    },
                    onNavigateToMain = { navigateToMain() },
                )
            }
        }
    }

    private fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    saveNotificationPermissionGranted()
                    onResult(true)
                }
                else -> {
                    notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            saveNotificationPermissionGranted()
            onResult(true)
        }
    }

    private fun saveNotificationPermissionGranted() {
        val factory = ViewModelFactory(this)
        ViewModelProvider(this, factory)[SettingsViewModel::class.java].setNotificationsEnabled(true)
    }

    private fun navigateToMain() {
        val nextIntent = Intent(this, com.example.al_aalim.ui.main.ContainerActivity::class.java)
        nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(nextIntent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
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
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(com.example.al_aalim.utils.LanguageManager.applyLanguage(newBase))
    }
}

// ─────────────────────────────────────────────────
// Compose UI
// ─────────────────────────────────────────────────

private enum class NotifDialogType { NONE, RATIONALE, DENIED }

@Composable
private fun NotificationPermissionScreen(
    onRequestPermission: (onResult: (Boolean) -> Unit) -> Unit,
    onNavigateToMain: () -> Unit,
) {
    var dialogType by remember { mutableStateOf(NotifDialogType.NONE) }

    fun handleEnable() {
        onRequestPermission { isGranted ->
            if (isGranted) {
                onNavigateToMain()
            } else {
                dialogType = NotifDialogType.DENIED
            }
        }
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
        Image(
            painter = painterResource(R.drawable.mosque_skyline_new),
            contentDescription = "Mosque silhouette",
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = mosqueAlpha }
        )

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp)
                .graphicsLayer { alpha = contentAlpha; translationY = contentOffset },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(0.5f))

            // Animated Reminder Radar
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
                        painter = painterResource(id = R.drawable.ic_settings_notification),
                        contentDescription = "Notification icon",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            Text(
                text = "Stay Connected",
                color = Gold,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Get timely reminders for prayers and important Islamic events.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.weight(0.8f))

            // Enable notifications button (Premium Style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { handleEnable() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enable Notifications",
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

        // Dialogs
        when (dialogType) {
            NotifDialogType.RATIONALE -> PermissionDialog(
                title = "📬 Stay Updated",
                message = "Notifications help you:\n\n• Never miss prayer times\n• Remember important Islamic dates\n• Receive daily spiritual guidance\n\nYou can always change this in settings later.",
                iconRes = R.drawable.ic_settings_notification,
                confirmText = "Allow",
                cancelText = "Not Now",
                onConfirm = { dialogType = NotifDialogType.NONE; /* launcher re-triggered */ },
                onCancel = { dialogType = NotifDialogType.NONE; onNavigateToMain() },
                onDismiss = { dialogType = NotifDialogType.NONE }
            )
            NotifDialogType.DENIED -> PermissionDialog(
                title = "📬 Notifications Disabled",
                message = "You won't receive:\n\n• Prayer time reminders\n• Islamic calendar notifications\n• Daily Quran verses and Hadith\n\nYou can enable notifications anytime in Settings.",
                iconRes = R.drawable.ic_settings_notification,
                confirmText = "Continue",
                cancelText = "Try Again",
                onConfirm = { dialogType = NotifDialogType.NONE; onNavigateToMain() },
                onCancel = { dialogType = NotifDialogType.NONE; handleEnable() },
                onDismiss = { dialogType = NotifDialogType.NONE }
            )
            NotifDialogType.NONE -> {}
        }
    }
}

