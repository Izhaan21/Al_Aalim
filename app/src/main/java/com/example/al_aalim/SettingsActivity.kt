package com.example.al_aalim

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.adapter.LanguageAdapter
import com.example.al_aalim.databinding.ActivitySettingsBinding
import com.example.al_aalim.model.Language
import com.example.al_aalim.utils.LanguageManager
import com.example.al_aalim.utils.CustomDialog

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var sharedPreferences: android.content.SharedPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize View Binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
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
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }
        
        setupClickListeners()
        updateLanguageDisplay()
    }
    
    private fun setupClickListeners() {
        // Setup back button
        binding.ivBack.setOnClickListener {
            finish()
        }
        
        // Setup language selection
        binding.btnLanguage.setOnClickListener {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            intent.putExtra("from_settings", true)
            startActivity(intent)
        }
        
        // Setup theme selection
        binding.btnTheme.setOnClickListener {
            showThemeSelectionDialog()
        }
        
        // Setup notifications switch
        // Load saved notification preference
        val notificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true)
        binding.switchNotifications.isChecked = notificationsEnabled
        
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Save preference
            sharedPreferences.edit().putBoolean("notifications_enabled", isChecked).apply()
            
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
            
            // TODO: Enable/disable actual notification channels
        }
        
        // Setup reciter selection
        binding.btnReciter.setOnClickListener {
            val intent = Intent(this, ReciterSelectionActivity::class.java)
            startActivity(intent)
        }
        
        // Setup Quran script selection
        binding.btnQuranScript.setOnClickListener {
            showScriptSelectionDialog()
        }
        
        // Setup prayer times settings
        binding.btnPrayerTimes.setOnClickListener {
            android.widget.Toast.makeText(this, "Prayer Times Settings - Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Implement prayer times calculation method settings
        }
        
        // Setup Qibla settings
        binding.btnQiblaSettings.setOnClickListener {
            android.widget.Toast.makeText(this, "Qibla Settings - Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Implement Qibla compass calibration settings
        }
        
        // Setup location settings
        binding.btnLocationSettings.setOnClickListener {
            android.widget.Toast.makeText(this, "Location Settings - Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Implement location permission management
        }
        
        // Setup font size
        binding.btnFontSize.setOnClickListener {
            android.widget.Toast.makeText(this, "Font Size Settings - Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Implement font size adjustment
        }
        
        // Setup Support & Community section
        binding.btnRateUs.setOnClickListener {
            openPlayStore()
        }
        
        binding.btnShareApp.setOnClickListener {
            shareApp()
        }
        
        binding.btnSupportUs.setOnClickListener {
            android.widget.Toast.makeText(this, "Support Us - Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Implement donation/remove ads options
        }
        
        binding.btnWhatsapp.setOnClickListener {
            openWhatsAppSupport()
        }
        
        binding.btnFollowUs.setOnClickListener {
            showSocialMediaDialog()
        }
        
        // Setup App Information section
        binding.btnFaq.setOnClickListener {
            android.widget.Toast.makeText(this, "FAQ - Coming Soon", android.widget.Toast.LENGTH_SHORT).show()
            // TODO: Open FAQ activity
        }
        
        binding.btnPrivacyPolicy.setOnClickListener {
            openUrl("https://yourapp.com/privacy")
        }
        
        binding.btnTerms.setOnClickListener {
            openUrl("https://yourapp.com/terms")
        }
        
        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
        
        // Setup clear chat history
        binding.btnClearHistory.setOnClickListener {
            showClearHistoryConfirmation()
        }
        
        // Setup clear cache
        binding.btnCache.setOnClickListener {
            showClearCacheConfirmation()
        }

        
        // Setup terms of service
        binding.btnTerms.setOnClickListener {
            openTermsOfService()
        }
    }
    
    private fun updateLanguageDisplay() {
        // Find the language text view and update it with current language
        val currentLanguage = LanguageManager.getSelectedLanguage(this)
        val languageNative = LanguageManager.getLanguageNativeName(currentLanguage)
        val languageEnglish = LanguageManager.getLanguageEnglishName(currentLanguage)
        
        // You can add a TextView to display current language in the layout
        // For now, we'll just prepare the data
    }
    
    private fun showLanguageSelectionDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_language_selection)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val recyclerView = dialog.findViewById<RecyclerView>(R.id.language_list)
        val btnClose = dialog.findViewById<TextView>(R.id.btn_close)
        val dialogTitle = dialog.findViewById<TextView>(R.id.dialog_title)
        
        dialogTitle.text = "Select Language"
        
        val languages = getAllLanguages()
        val currentLanguage = LanguageManager.getSelectedLanguage(this)
        
        val adapter = LanguageAdapter(languages) { selectedLanguage ->
            // Save the selected language
            LanguageManager.setLanguage(this, selectedLanguage.code)
            
            // Show restart dialog or reload app
            dialog.dismiss()
            showRestartDialog()
        }
        
        adapter.setSelectedLanguage(currentLanguage)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun showRestartDialog() {
        com.example.al_aalim.utils.CustomDialog.showConfirmation(
            context = this,
            title = "Language Changed",
            message = "The app needs to restart to apply the new language. Restart now?",
            confirmText = "Restart",
            cancelText = "Later",
            onConfirm = {
                restartApp()
            }
        )
    }
    
    private fun restartApp() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
        Runtime.getRuntime().exit(0)
    }
    
    private fun getAllLanguages(): List<Language> {
        return listOf(
            // Primary Islamic languages
            Language("ar", "العربية", "Arabic", "🇸🇦"),
            Language("en", "English", "English", "🇺🇸"),
            Language("ur", "اردو", "Urdu", "🇵🇰"),
            
            // South Asian languages
            Language("bn", "বাংলা", "Bengali", "🇧🇩"),
            Language("hi", "हिन्दी", "Hindi", "🇮🇳"),
            
            // Southeast Asian languages
            Language("id", "Bahasa Indonesia", "Indonesian", "🇮🇩"),
            Language("ms", "Bahasa Melayu", "Malay", "🇲🇾"),
            
            // African languages
            Language("ha", "Hausa", "Hausa", "🇳🇬"),
            Language("sw", "Kiswahili", "Swahili", "🇰🇪"),
            
            // Middle Eastern languages
            Language("fa", "فارسی", "Persian", "🇮🇷"),
            Language("tr", "Türkçe", "Turkish", "🇹🇷"),
            
            // European languages
            Language("fr", "Français", "French", "🇫🇷"),
            Language("de", "Deutsch", "German", "🇩🇪"),
            Language("es", "Español", "Spanish", "🇪🇸"),
            
            // Central Asian languages
            Language("uz", "O'zbek", "Uzbek", "🇺🇿"),
            
            // Other significant languages
            Language("zh", "中文", "Chinese", "🇨🇳"),
            Language("ru", "Русский", "Russian", "🇷🇺")
        )
    }
    
    private fun showClearHistoryConfirmation() {
        com.example.al_aalim.utils.CustomDialog.showConfirmation(
            context = this,
            title = "Clear Chat History",
            message = "Are you sure you want to delete all your conversations? This action cannot be undone.",
            confirmText = "Clear",
            cancelText = "Cancel",
            onConfirm = {
                // TODO: Implement actual chat history clearing
                android.widget.Toast.makeText(this, "Chat history cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun showClearCacheConfirmation() {
        com.example.al_aalim.utils.CustomDialog.showConfirmation(
            context = this,
            title = "Clear Cache",
            message = "This will free up storage space by removing temporary files. Continue?",
            confirmText = "Clear",
            cancelText = "Cancel",
            onConfirm = {
                try {
                    // Clear app cache
                    cacheDir.deleteRecursively()
                    android.widget.Toast.makeText(this, "Cache cleared successfully", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Failed to clear cache", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
    
    private fun showThemeSelectionDialog() {
        val themes = arrayOf("Light", "Dark", "Follow System")
        val themeValues = arrayOf(
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO,
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES,
            androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        )
        
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentTheme = sharedPreferences.getInt("app_theme", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        val currentIndex = themeValues.indexOf(currentTheme)
        
        com.example.al_aalim.utils.CustomDialog.showSelectionList(
            context = this,
            title = "Select Theme",
            options = themes.toList(),
            selectedIndex = currentIndex,
            onSelect = { which, _ ->
                val selectedTheme = themeValues[which]
                
                // Save preference
                sharedPreferences.edit().putInt("app_theme", selectedTheme).apply()
                
                // Apply theme
                androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(selectedTheme)
                android.widget.Toast.makeText(this, "Theme changed to ${themes[which]}", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun showReciterSelectionDialog() {
        val reciters = arrayOf(
            "Abdul Basit Abdul Samad",
            "Mishary Rashid Alafasy",
            "Saad Al-Ghamdi",
            "Abdul Rahman Al-Sudais",
            "Maher Al-Muaiqly",
            "Ahmed Al-Ajmy",
            "Muhammad Siddiq Al-Minshawi",
            "Ali Abdur-Rahman Al-Huthaify"
        )
        
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentReciter = sharedPreferences.getString("quran_reciter", reciters[0])
        val currentIndex = reciters.indexOf(currentReciter)
        
        com.example.al_aalim.utils.CustomDialog.showSelectionList(
            context = this,
            title = "Select Quran Reciter",
            options = reciters.toList(),
            selectedIndex = currentIndex,
            onSelect = { which, selectedReciter ->
                // Save preference
                sharedPreferences.edit().putString("quran_reciter", selectedReciter).apply()
                android.widget.Toast.makeText(this, "Reciter: $selectedReciter", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun showScriptSelectionDialog() {
        val scripts = arrayOf("Uthmani", "Indopak", "Simple Enhanced")
        
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val currentScript = sharedPreferences.getString("quran_script", scripts[0])
        val currentIndex = scripts.indexOf(currentScript)
        
        com.example.al_aalim.utils.CustomDialog.showSelectionList(
            context = this,
            title = "Select Quran Script",
            options = scripts.toList(),
            selectedIndex = currentIndex,
            onSelect = { which, selectedScript ->
                // Save preference
                sharedPreferences.edit().putString("quran_script", selectedScript).apply()
                android.widget.Toast.makeText(this, "Script: $selectedScript", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }
    
    private fun openPrivacyPolicy() {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("https://yourwebsite.com/privacy-policy")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Unable to open privacy policy", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openTermsOfService() {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("https://yourwebsite.com/terms-of-service")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Unable to open terms of service", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("market://details?id=$packageName")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // If Play Store app not available, open in browser
            intent.data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            startActivity(intent)
        }
    }
    
    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Al-Aalim - Islamic Companion")
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out Al-Aalim, your Islamic companion app!\nhttps://play.google.com/store/apps/details?id=$packageName")
        startActivity(Intent.createChooser(shareIntent, "Share Al-Aalim"))
    }
    
    private fun openWhatsAppSupport() {
        val phoneNumber = "+1234567890" // Replace with actual support number
        val message = "Hello, I need help with Al-Aalim app"
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("https://wa.me/$phoneNumber?text=${android.net.Uri.encode(message)}")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "WhatsApp not installed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showSocialMediaDialog() {
        val options = arrayOf("Facebook", "Instagram", "Twitter", "YouTube")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Follow Us On")
        builder.setItems(options) { _, which ->
            val url = when (which) {
                0 -> "https://facebook.com/alaalim"
                1 -> "https://instagram.com/alaalim"
                2 -> "https://twitter.com/alaalim"
                3 -> "https://youtube.com/@alaalim"
                else -> ""
            }
            openUrl(url)
        }
        builder.show()
    }
    
    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse(url)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Unable to open link", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showAboutDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("About Al-Aalim")
        builder.setMessage("Al-Aalim - Your Islamic Companion\n\nVersion: 1.0.0\n\nDeveloped with ❤️ to help Muslims strengthen their faith and practice.\n\n© 2026 Al-Aalim. All rights reserved.")
        builder.setPositiveButton("OK", null)
        builder.show()
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}
