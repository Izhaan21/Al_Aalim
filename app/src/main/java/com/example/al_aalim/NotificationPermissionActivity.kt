package com.example.al_aalim

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.utils.HapticUtils
import com.example.al_aalim.utils.HapticUtils.haptic

class NotificationPermissionActivity : AppCompatActivity() {

    private lateinit var btnEnableNotifications: TextView
    private lateinit var btnSkip: TextView
    private lateinit var loadingOverlay: View

    private val notificationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            saveNotificationPermissionGranted()
            navigateToMain()
        } else {
            // Permission denied - show explanation dialog
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_notification_permission)

        // Handle system bars insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hideSystemBars()
        initViews()
        setupClickListeners()
        animateViews()
    }

    private fun initViews() {
        btnEnableNotifications = findViewById(R.id.btn_enable_notifications)
        btnSkip = findViewById(R.id.btn_skip)
        loadingOverlay = findViewById(R.id.loading_overlay)
    }

    private fun setupClickListeners() {
        btnEnableNotifications.setOnClickWithAnimation {
            btnEnableNotifications.haptic(HapticUtils.HapticType.MEDIUM_CLICK)
            requestNotificationPermission()
        }

        btnSkip.setOnClickWithAnimation {
            btnSkip.haptic(HapticUtils.HapticType.LIGHT_TAP)
            navigateToMain()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted
                    saveNotificationPermissionGranted()
                    navigateToMain()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show rationale before requesting
                    showPermissionRationaleDialog()
                }
                else -> {
                    // Request permission
                    notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 and below don't need runtime permission
            saveNotificationPermissionGranted()
            navigateToMain()
        }
    }

    private fun showPermissionRationaleDialog() {
        showBottomSheetDialog(
            title = "📬 Stay Updated",
            message = "Notifications help you:\n\n" +
                    "• Never miss prayer times\n" +
                    "• Remember important Islamic dates\n" +
                    "• Receive daily spiritual guidance\n\n" +
                    "You can always change this in settings later.",
            positiveText = "Allow",
            negativeText = "Not Now",
            onPositive = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onNegative = {
                navigateToMain()
            }
        )
    }

    private fun showPermissionDeniedDialog() {
        showBottomSheetDialog(
            title = "📬 Notifications Disabled",
            message = "You won't receive:\n\n" +
                    "• Prayer time reminders\n" +
                    "• Islamic calendar notifications\n" +
                    "• Daily Quran verses and Hadith\n\n" +
                    "You can enable notifications anytime in Settings.",
            positiveText = "Continue",
            negativeText = "Try Again",
            onPositive = {
                navigateToMain()
            },
            onNegative = {
                requestNotificationPermission()
            }
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

    private fun saveNotificationPermissionGranted() {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("notification_permission_granted", true).apply()
    }

    private fun navigateToMain() {
        val intent = Intent(this, ContainerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
        }
    }

    private fun animateViews() {
        // Fade in animation for glass card and buttons
        val glassCard = findViewById<View>(R.id.glass_card)
        val illustration = findViewById<View>(R.id.iv_illustration)
        val mosque = findViewById<View>(R.id.iv_mosque_silhouette)

        glassCard.alpha = 0f
        illustration.alpha = 0f
        mosque.alpha = 0f

        glassCard.animate().alpha(1f).setDuration(600).start()
        illustration.animate().alpha(1f).setDuration(800).setStartDelay(200).start()
        mosque.animate().alpha(0.3f).setDuration(1000).setStartDelay(400).start()
    }
}
