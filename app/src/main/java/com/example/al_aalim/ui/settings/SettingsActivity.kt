package com.example.al_aalim.ui.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.example.al_aalim.R
import com.example.al_aalim.ui.auth.WelcomeActivity
import com.example.al_aalim.ui.components.AboutDialog
import com.example.al_aalim.ui.components.ConfirmationDialog
import com.example.al_aalim.ui.components.SelectionListDialog
import com.example.al_aalim.ui.components.SupportUsDialog
import com.example.al_aalim.ui.features.ReciterSelectionActivity
import com.example.al_aalim.ui.theme.AlAalimTheme
import com.example.al_aalim.utils.LanguageManager
import com.example.al_aalim.viewmodel.SettingsViewModel
import com.example.al_aalim.viewmodel.ViewModelFactory

class SettingsActivity : ComponentActivity() {

    private lateinit var viewModel: SettingsViewModel
    private lateinit var accountViewModel: com.example.al_aalim.viewmodel.AccountViewModel

    private val themes = listOf("Light", "Dark", "Follow System")
    private val themeValues = listOf(
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
        androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )
    private val reciters = listOf(
        "Abdul Basit Abdul Samad", "Mishary Rashid Alafasy", "Saad Al-Ghamdi",
        "Abdul Rahman Al-Sudais",  "Maher Al-Muaiqly",       "Ahmed Al-Ajmy",
        "Muhammad Siddiq Al-Minshawi", "Ali Abdur-Rahman Al-Huthaify"
    )
    private val scripts       = listOf("Uthmani", "Indopak", "Simple Enhanced")
    private val socialOptions = listOf("Facebook", "Instagram", "Twitter", "LinkedIn")
    private val socialIcons   = listOf(
        R.drawable.ic_facebook, R.drawable.ic_instagram,
        R.drawable.ic_twitter,  R.drawable.ic_linkedin
    )
    private val socialUrls = listOf(
        "https://facebook.com/alaalim",  "https://instagram.com/alaalim",
        "https://twitter.com/alaalim",   "https://linkedin.com/company/alaalim"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val factory = ViewModelFactory(this)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
        accountViewModel = ViewModelProvider(this, factory)[com.example.al_aalim.viewmodel.AccountViewModel::class.java]
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
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
                val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = true)
                
                // Dialog States
                var showAboutDlg by remember { mutableStateOf(false) }
                var showSupportDlg by remember { mutableStateOf(false) }
                var showRestartDlg by remember { mutableStateOf(false) }
                var showClearHistDlg by remember { mutableStateOf(false) }
                var showClearCacheDlg by remember { mutableStateOf(false) }
                var showThemeDlg by remember { mutableStateOf(false) }
                var showReciterDlg by remember { mutableStateOf(false) }
                var showScriptDlg by remember { mutableStateOf(false) }
                var showSocialDlg by remember { mutableStateOf(false) }

                androidx.compose.runtime.LaunchedEffect(Unit) {
                    accountViewModel.messageEvent.collect { message ->
                        message?.let {
                            android.widget.Toast.makeText(this@SettingsActivity, it, android.widget.Toast.LENGTH_SHORT).show()
                            accountViewModel.clearMessage()
                        }
                    }
                }

                val startDest = intent.getStringExtra("start_destination") ?: SettingsRoute.Main.route

                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(com.example.al_aalim.ui.theme.BackgroundGradientStart, com.example.al_aalim.ui.theme.BackgroundGradientEnd)))) {
                    SettingsNavGraph(
                        startDestination = startDest,
                        onFinish = { finish() },
                        notificationsEnabled = notificationsEnabled,
                        onNotificationsChanged = { enabled ->
                            viewModel.setNotificationsEnabled(enabled)
                            android.widget.Toast.makeText(this@SettingsActivity, if(enabled) "Notifications enabled" else "Notifications disabled", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onNavigateToReciter = {
                            startActivity(Intent(this@SettingsActivity, ReciterSelectionActivity::class.java))
                        },
                        onShowDialog = { dialogId ->
                            when (dialogId) {
                                "about" -> showAboutDlg = true
                                "support" -> showSupportDlg = true
                                "clear_history" -> showClearHistDlg = true
                                "clear_cache" -> showClearCacheDlg = true
                                "theme" -> showThemeDlg = true
                                "reciter" -> showReciterDlg = true
                                "script" -> showScriptDlg = true
                                "social", "follow" -> showSocialDlg = true
                                "rate" -> openPlayStore()
                                "share" -> shareApp()
                                "whatsapp" -> openWhatsAppSupport()
                                "terms" -> openTermsOfService()
                            }
                        }
                    )

                    // Overlay Dialogs Native to Compose
                    if (showAboutDlg) {
                        AboutDialog(
                            appName = getString(R.string.app_name), version = "1.0.0",
                            description = "Al-Aalim is your comprehensive Islamic companion, designed to help you stay connected with your faith through the Quran, authentic prayers, and daily spiritual guidance.",
                            onDismiss = { showAboutDlg = false }
                        )
                    }

                    if (showSupportDlg) {
                        SupportUsDialog(
                            onTierSelected = { _, price ->
                                android.widget.Toast.makeText(this@SettingsActivity, "JazakAllah Khair! $price support selected. Billing integration coming soon.", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { showSupportDlg = false }
                        )
                    }

                    if (showRestartDlg) {
                        ConfirmationDialog(
                            title = "Language Changed",
                            message = "The app needs to restart to apply the new language. Restart now?",
                            confirmText = "Restart", cancelText = "Later",
                            onConfirm = { restartApp() },
                            onDismiss = { showRestartDlg = false }
                        )
                    }

                    if (showClearHistDlg) {
                        ConfirmationDialog(
                            title = "Clear Chat History",
                            message = "Are you sure you want to delete all your conversations? This action cannot be undone.",
                            confirmText = "Clear", cancelText = "Cancel",
                            onConfirm = {
                                accountViewModel.deleteChatHistory()
                                showClearHistDlg = false
                            },
                            onDismiss = { showClearHistDlg = false }
                        )
                    }

                    if (showClearCacheDlg) {
                        ConfirmationDialog(
                            title = "Clear Cache",
                            message = "This will free up storage space by removing temporary files. Continue?",
                            confirmText = "Clear", cancelText = "Cancel",
                            onConfirm = {
                                accountViewModel.clearCache(this@SettingsActivity)
                                showClearCacheDlg = false
                            },
                            onDismiss = { showClearCacheDlg = false }
                        )
                    }

                    if (showThemeDlg) {
                        SelectionListDialog(
                            title = "Select Theme", options = themes,
                            selectedIndex = themeValues.indexOf(viewModel.appTheme.value), autoApply = true,
                            onSelect = { which, _ ->
                                viewModel.setTheme(themeValues[which])
                                android.widget.Toast.makeText(this@SettingsActivity, "Theme changed to ${themes[which]}", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { showThemeDlg = false }
                        )
                    }

                    if (showReciterDlg) {
                        SelectionListDialog(
                            title = "Select Quran Reciter", options = reciters,
                            selectedIndex = reciters.indexOf(viewModel.quranReciter.value), autoApply = true,
                            onSelect = { _, selected ->
                                viewModel.setQuranReciter(selected)
                                android.widget.Toast.makeText(this@SettingsActivity, "Reciter: $selected", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { showReciterDlg = false }
                        )
                    }

                    if (showScriptDlg) {
                        SelectionListDialog(
                            title = "Select Quran Script", options = scripts,
                            selectedIndex = scripts.indexOf(viewModel.quranScript.value), autoApply = true,
                            onSelect = { _, selected ->
                                viewModel.setQuranScript(selected)
                                android.widget.Toast.makeText(this@SettingsActivity, "Script: $selected", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            onDismiss = { showScriptDlg = false }
                        )
                    }

                    if (showSocialDlg) {
                        SelectionListDialog(
                            title = "Follow Us On", subtitle = "Keep in touch with us on social media",
                            iconRes = R.drawable.ic_settings_follow, options = socialOptions,
                            optionIconRes = socialIcons, selectedIndex = -1, autoApply = true, showRadio = false,
                            onSelect = { which, _ -> openUrl(socialUrls[which]) },
                            onDismiss = { showSocialDlg = false }
                        )
                    }
                }
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
        Runtime.getRuntime().exit(0)
    }

    private fun openTermsOfService() {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("https://yourwebsite.com/terms-of-service")
        try { startActivity(intent) } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Unable to open terms of service", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("market://details?id=$packageName")
        try { startActivity(intent) } catch (e: Exception) {
            intent.data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            startActivity(intent)
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Al-Aalim - Islamic Companion")
            putExtra(Intent.EXTRA_TEXT, "Check out Al-Aalim, your Islamic companion app!\nhttps://play.google.com/store/apps/details?id=$packageName")
        }
        startActivity(Intent.createChooser(shareIntent, "Share Al-Aalim"))
    }

    private fun openWhatsAppSupport() {
        val phoneNumber = "+1234567890"
        val message = "Hello, I need help with Al-Aalim app"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("https://wa.me/$phoneNumber?text=${android.net.Uri.encode(message)}")
        try { startActivity(intent) } catch (e: Exception) {
            android.widget.Toast.makeText(this, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse(url)
        try { startActivity(intent) } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Unable to open link", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }

    override fun onResume() {
        super.onResume()
        if (::accountViewModel.isInitialized) {
            accountViewModel.loadProfile()
        }
    }
}