package com.example.al_aalim

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.al_aalim.adapter.LanguageAdapter
import com.example.al_aalim.model.Language

class LanguageSelectionActivity : AppCompatActivity() {

    private var selectedLanguage: Language? = null
    private var isListVisible = false

    // Views
    private lateinit var centeredContainer: LinearLayout
    private lateinit var listContainer: LinearLayout
    private lateinit var compactSearchInput: EditText
    private lateinit var compactClearSearch: ImageView
    private lateinit var languageList: RecyclerView
    private lateinit var btnContinue: TextView
    private lateinit var btnSelectLanguage: TextView
    private lateinit var resultsCount: TextView
    private lateinit var globeContainer: View
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView

    private lateinit var languageAdapter: LanguageAdapter
    private lateinit var allLanguages: List<Language>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContentView(R.layout.activity_language_selection)

        // Handle system bars insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        hideSystemBars()
        initLanguages()
        initViews()
        setupRecyclerView()
        setupSearchListeners()
        
        // Automatically show language list when coming from Settings
        if (intent.getBooleanExtra("from_settings", false) || true) {
            // Hide centered container and show list immediately
            centeredContainer.visibility = View.GONE
            listContainer.visibility = View.VISIBLE
            isListVisible = true
            
            // Focus on search input
            compactSearchInput.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(compactSearchInput, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun initLanguages() {
        allLanguages = listOf(
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

    private fun initViews() {
        centeredContainer = findViewById(R.id.centered_container)
        listContainer = findViewById(R.id.list_container)
        compactSearchInput = findViewById(R.id.compact_search_input)
        compactClearSearch = findViewById(R.id.compact_clear_search)
        languageList = findViewById(R.id.language_list)
        btnContinue = findViewById(R.id.btn_continue)
        btnSelectLanguage = findViewById(R.id.btn_select_language)
        resultsCount = findViewById(R.id.results_count)
        globeContainer = findViewById(R.id.globe_container)
        titleText = findViewById(R.id.title_text)
        subtitleText = findViewById(R.id.subtitle_text)
        
        // Set up select language button click
        btnSelectLanguage.setOnClickListener {
            showLanguageList()
        }
    }

    private fun setupRecyclerView() {
        languageAdapter = LanguageAdapter(allLanguages) { language ->
            selectedLanguage = language
            // Show continue button when language is selected
            showContinueButton()
        }
        
        languageList.layoutManager = LinearLayoutManager(this)
        languageList.adapter = languageAdapter
    }

    private fun setupSearchListeners() {
        // Compact search input (list view only)
        compactSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                filterLanguages(query)
                compactClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            }
        })

        compactClearSearch.setOnClickListener {
            compactSearchInput.text.clear()
        }

        // Continue button
        btnContinue.setOnClickListener {
            if (selectedLanguage != null) {
                saveLanguagePreference()
                applyLanguageAndProceed()
            }
        }
    }

    private fun filterLanguages(query: String) {
        val filtered = if (query.isEmpty()) {
            allLanguages
        } else {
            allLanguages.filter { language ->
                language.nativeName.contains(query, ignoreCase = true) ||
                language.englishName.contains(query, ignoreCase = true) ||
                language.code.contains(query, ignoreCase = true)
            }
        }
        
        languageAdapter.updateLanguages(filtered)
        updateResultsCount(filtered.size)
    }

    private fun updateResultsCount(count: Int) {
        resultsCount.text = when (count) {
            0 -> "No languages found"
            1 -> "1 language found"
            else -> "$count languages found"
        }
    }

    private fun showLanguageList() {
        if (isListVisible) return
        isListVisible = true

        // Show list container (continue button stays hidden until language selected)
        listContainer.visibility = View.VISIBLE

        // Animate globe, title, subtitle out (fade out)
        val globeFade = ObjectAnimator.ofFloat(globeContainer, "alpha", 1f, 0f)
        val titleFade = ObjectAnimator.ofFloat(titleText, "alpha", 1f, 0f)
        val subtitleFade = ObjectAnimator.ofFloat(subtitleText, "alpha", 1f, 0f)

        // Animate centered container out (move up and fade)
        val centeredMoveUp = ObjectAnimator.ofFloat(centeredContainer, "translationY", 0f, -200f)
        val centeredFade = ObjectAnimator.ofFloat(centeredContainer, "alpha", 1f, 0f)
        
        // Animate select language button out
        val btnSelectFade = ObjectAnimator.ofFloat(btnSelectLanguage, "alpha", 1f, 0f)
        val btnSelectMoveDown = ObjectAnimator.ofFloat(btnSelectLanguage, "translationY", 0f, 50f)

        // Animate list container in (slide up and fade in)
        listContainer.translationY = 100f
        val listMoveUp = ObjectAnimator.ofFloat(listContainer, "translationY", 100f, 0f)
        val listFade = ObjectAnimator.ofFloat(listContainer, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            globeFade, titleFade, subtitleFade,
            centeredMoveUp, centeredFade,
            btnSelectFade, btnSelectMoveDown,
            listMoveUp, listFade
        )
        animatorSet.duration = 400
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()

        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                centeredContainer.visibility = View.GONE
                globeContainer.visibility = View.GONE
                titleText.visibility = View.GONE
                subtitleText.visibility = View.GONE
                btnSelectLanguage.visibility = View.GONE
                
                // Focus compact search input
                compactSearchInput.requestFocus()
                // Start with empty search
                compactSearchInput.setText("")
            }
        })

        // Show all languages initially
        updateResultsCount(allLanguages.size)
    }

    private fun saveLanguagePreference() {
        selectedLanguage?.let { language ->
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_language", language.code).apply()
            
            // Also save using LanguageManager
            com.example.al_aalim.utils.LanguageManager.setLanguage(this, language.code)
        }
    }
    
    private fun applyLanguageAndProceed() {
        selectedLanguage?.let { language ->
            // Apply language using LanguageManager
            com.example.al_aalim.utils.LanguageManager.applyLanguage(this, language.code)
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        // Navigate to the main screen after language selection
        val intent = Intent(this, NotificationPermissionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
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
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isListVisible) {
            // Animate back to centered view
            hideLanguageList()
        } else {
            super.onBackPressed()
        }
    }

    private fun showContinueButton() {
        if (btnContinue.visibility == View.VISIBLE) return
        
        btnContinue.visibility = View.VISIBLE
        btnContinue.alpha = 0f
        btnContinue.translationY = 50f
        
        btnContinue.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideLanguageList() {
        isListVisible = false

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(compactSearchInput.windowToken, 0)

        // Show centered container and globe/title/subtitle
        centeredContainer.visibility = View.VISIBLE
        centeredContainer.alpha = 0f
        centeredContainer.translationY = -200f
        
        globeContainer.visibility = View.VISIBLE
        globeContainer.alpha = 0f
        titleText.visibility = View.VISIBLE
        titleText.alpha = 0f
        subtitleText.visibility = View.VISIBLE
        subtitleText.alpha = 0f
        
        btnSelectLanguage.visibility = View.VISIBLE
        btnSelectLanguage.alpha = 0f
        btnSelectLanguage.translationY = 50f

        // Animate globe, title, subtitle back in
        val globeFade = ObjectAnimator.ofFloat(globeContainer, "alpha", 0f, 1f)
        val titleFade = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f)
        val subtitleFade = ObjectAnimator.ofFloat(subtitleText, "alpha", 0f, 1f)

        // Animate centered container back in
        val centeredMoveDown = ObjectAnimator.ofFloat(centeredContainer, "translationY", -200f, 0f)
        val centeredFade = ObjectAnimator.ofFloat(centeredContainer, "alpha", 0f, 1f)
        
        // Animate select button back in
        val btnSelectFade = ObjectAnimator.ofFloat(btnSelectLanguage, "alpha", 0f, 1f)
        val btnSelectMoveUp = ObjectAnimator.ofFloat(btnSelectLanguage, "translationY", 50f, 0f)

        // Animate list out
        val listMoveDown = ObjectAnimator.ofFloat(listContainer, "translationY", 0f, 100f)
        val listFade = ObjectAnimator.ofFloat(listContainer, "alpha", 1f, 0f)

        // Animate button out
        val btnMoveDown = ObjectAnimator.ofFloat(btnContinue, "translationY", 0f, 50f)
        val btnFade = ObjectAnimator.ofFloat(btnContinue, "alpha", 1f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            globeFade, titleFade, subtitleFade,
            centeredMoveDown, centeredFade,
            btnSelectFade, btnSelectMoveUp,
            listMoveDown, listFade,
            btnMoveDown, btnFade
        )
        animatorSet.duration = 350
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()

        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                listContainer.visibility = View.GONE
                btnContinue.visibility = View.GONE
                
                // Clear search
                compactSearchInput.text.clear()
            }
        })
    }
}
