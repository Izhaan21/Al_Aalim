package com.example.al_aalim.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.ui.auth.WelcomeActivity
import com.example.al_aalim.ui.permissions.NotificationPermissionActivity
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.utils.LanguageManager

class LanguageSelectionActivity : ComponentActivity() {

    private lateinit var windowInsetsController: WindowInsetsControllerCompat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle "from_settings" and "from_auth" flags
        val isFromSettings = intent.getBooleanExtra("from_settings", false)
        val isFromAuth = intent.getBooleanExtra("from_auth", false)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            AlAalimTheme {
                LanguageSelectionScreen(
                    onBack = { finish() },
                    showBackButton = isFromSettings,
                    onLanguageSelected = { selected ->
                        LanguageManager.setLanguage(this@LanguageSelectionActivity, selected.code)

                        when {
                            isFromSettings -> {
                                android.widget.Toast.makeText(
                                    this@LanguageSelectionActivity,
                                    "Language updated, restart required.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                val restartIntent = Intent(this@LanguageSelectionActivity, WelcomeActivity::class.java)
                                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(restartIntent)
                                finish()
                                Runtime.getRuntime().exit(0)
                            }
                            isFromAuth -> {
                                // Post-auth onboarding: proceed to main app
                                val mainIntent = Intent(this@LanguageSelectionActivity, com.example.al_aalim.ui.main.ContainerActivity::class.java)
                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(mainIntent)
                                finish()
                            }
                            else -> {
                                // Pre-login onboarding: return to Welcome screen
                                val mainIntent = Intent(this@LanguageSelectionActivity, WelcomeActivity::class.java)
                                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(mainIntent)
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}
