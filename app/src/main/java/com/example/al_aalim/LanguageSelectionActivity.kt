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
    private lateinit var searchInput: EditText
    private lateinit var clearSearch: ImageView
    private lateinit var compactSearchInput: EditText
    private lateinit var compactClearSearch: ImageView
    private lateinit var languageList: RecyclerView
    private lateinit var btnContinue: TextView
    private lateinit var resultsCount: TextView

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
    }

    private fun initLanguages() {
        allLanguages = listOf(
            Language("ar", "العربية", "Arabic", R.drawable.flag_saudi_arabia),
            Language("en", "English", "English", R.drawable.flag_usa),
            Language("ur", "اردو", "Urdu", R.drawable.flag_pakistan),
            Language("fr", "Français", "French", R.drawable.flag_france)
        )
    }

    private fun initViews() {
        centeredContainer = findViewById(R.id.centered_container)
        listContainer = findViewById(R.id.list_container)
        searchInput = findViewById(R.id.search_input)
        clearSearch = findViewById(R.id.clear_search)
        compactSearchInput = findViewById(R.id.compact_search_input)
        compactClearSearch = findViewById(R.id.compact_clear_search)
        languageList = findViewById(R.id.language_list)
        btnContinue = findViewById(R.id.btn_continue)
        resultsCount = findViewById(R.id.results_count)
    }

    private fun setupRecyclerView() {
        languageAdapter = LanguageAdapter(allLanguages) { language ->
            selectedLanguage = language
        }
        
        languageList.layoutManager = LinearLayoutManager(this)
        languageList.adapter = languageAdapter
    }

    private fun setupSearchListeners() {
        // Main search input (centered view)
        searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !isListVisible) {
                showLanguageList()
            }
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (!isListVisible && query.isNotEmpty()) {
                    showLanguageList()
                }
                filterLanguages(query)
                clearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Sync with compact search
                if (compactSearchInput.text.toString() != query) {
                    compactSearchInput.setText(query)
                    compactSearchInput.setSelection(query.length)
                }
            }
        })

        clearSearch.setOnClickListener {
            searchInput.text.clear()
            compactSearchInput.text.clear()
        }

        // Compact search input (list view)
        compactSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                filterLanguages(query)
                compactClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                // Sync with main search
                if (searchInput.text.toString() != query) {
                    searchInput.setText(query)
                    searchInput.setSelection(query.length)
                }
            }
        })

        compactClearSearch.setOnClickListener {
            compactSearchInput.text.clear()
            searchInput.text.clear()
        }

        // Continue button
        btnContinue.setOnClickListener {
            if (selectedLanguage != null) {
                saveLanguagePreference()
                navigateToMain()
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

        // Hide keyboard from main search
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)

        // Show list container
        listContainer.visibility = View.VISIBLE
        btnContinue.visibility = View.VISIBLE

        // Animate centered container out (move up and fade)
        val centeredMoveUp = ObjectAnimator.ofFloat(centeredContainer, "translationY", 0f, -200f)
        val centeredFade = ObjectAnimator.ofFloat(centeredContainer, "alpha", 1f, 0f)

        // Animate list container in (slide up and fade in)
        listContainer.translationY = 100f
        val listMoveUp = ObjectAnimator.ofFloat(listContainer, "translationY", 100f, 0f)
        val listFade = ObjectAnimator.ofFloat(listContainer, "alpha", 0f, 1f)

        // Animate continue button in
        btnContinue.translationY = 50f
        val btnMoveUp = ObjectAnimator.ofFloat(btnContinue, "translationY", 50f, 0f)
        val btnFade = ObjectAnimator.ofFloat(btnContinue, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            centeredMoveUp, centeredFade,
            listMoveUp, listFade,
            btnMoveUp, btnFade
        )
        animatorSet.duration = 400
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()

        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                centeredContainer.visibility = View.GONE
                
                // Focus compact search input
                compactSearchInput.requestFocus()
                compactSearchInput.setText(searchInput.text)
                compactSearchInput.setSelection(compactSearchInput.text.length)
            }
        })

        // Show all languages initially
        updateResultsCount(allLanguages.size)
    }

    private fun saveLanguagePreference() {
        selectedLanguage?.let { language ->
            val prefs = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            prefs.edit().putString("selected_language", language.code).apply()
        }
    }

    private fun navigateToMain() {
        // Navigate to Location Permission screen after language selection
        val intent = Intent(this, LocationPermissionActivity::class.java)
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

    private fun hideLanguageList() {
        isListVisible = false

        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(compactSearchInput.windowToken, 0)

        // Show centered container
        centeredContainer.visibility = View.VISIBLE
        centeredContainer.alpha = 0f
        centeredContainer.translationY = -200f

        // Animate centered container back in
        val centeredMoveDown = ObjectAnimator.ofFloat(centeredContainer, "translationY", -200f, 0f)
        val centeredFade = ObjectAnimator.ofFloat(centeredContainer, "alpha", 0f, 1f)

        // Animate list out
        val listMoveDown = ObjectAnimator.ofFloat(listContainer, "translationY", 0f, 100f)
        val listFade = ObjectAnimator.ofFloat(listContainer, "alpha", 1f, 0f)

        // Animate button out
        val btnMoveDown = ObjectAnimator.ofFloat(btnContinue, "translationY", 0f, 50f)
        val btnFade = ObjectAnimator.ofFloat(btnContinue, "alpha", 1f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            centeredMoveDown, centeredFade,
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
                searchInput.text.clear()
                compactSearchInput.text.clear()
            }
        })
    }
}
