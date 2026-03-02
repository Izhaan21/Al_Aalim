package com.example.al_aalim

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.al_aalim.databinding.ActivityQuranBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation

class QuranActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuranBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var surahAdapter: SurahAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityQuranBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        supportActionBar?.hide()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            view.setPadding(0, 0, 0, 0)
            WindowInsetsCompat.CONSUMED
        }
        hideSystemBars()

        // RecyclerView
        surahAdapter = SurahAdapter(SurahAdapter.getAllSurahs())
        binding.rvSurahList.apply {
            layoutManager = LinearLayoutManager(this@QuranActivity)
            adapter = surahAdapter
            setHasFixedSize(false)
        }

        // Live search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                surahAdapter.filter.filter(s)
                binding.ivClearSearch.visibility =
                    if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear search
        binding.ivClearSearch.setOnClickListener {
            binding.etSearch.text.clear()
            binding.ivClearSearch.visibility = View.GONE
            surahAdapter.filter.filter("")
        }

        // Tabs
        binding.tabAll.setOnClickListener { selectTab(false) }
        binding.tabFavorites.setOnClickListener { selectTab(true) }

        setupNavigation()
        binding.ivBack.setOnClickListener { finish() }
        setActiveNavigation()
    }

    private fun selectTab(favouritesMode: Boolean) {
        val selectedColor = android.graphics.Color.BLACK
        val unselectedColor = android.graphics.Color.parseColor("#CCFFFFFF")
        if (favouritesMode) {
            binding.tabFavorites.setBackgroundResource(R.drawable.tab_selected_background)
            binding.tabFavorites.setTextColor(selectedColor)
            binding.tabAll.setBackgroundResource(R.drawable.surah_item_background)
            binding.tabAll.setTextColor(unselectedColor)
        } else {
            binding.tabAll.setBackgroundResource(R.drawable.tab_selected_background)
            binding.tabAll.setTextColor(selectedColor)
            binding.tabFavorites.setBackgroundResource(R.drawable.surah_item_background)
            binding.tabFavorites.setTextColor(unselectedColor)
        }
        surahAdapter.setFavouritesOnly(favouritesMode)
    }

    private fun setActiveNavigation() {
        binding.navBook.isSelected = true
        binding.ivNavBook.isSelected = true
        binding.tvNavBook.isSelected = true
        binding.navHome.isSelected = false
        binding.ivNavHome.isSelected = false
        binding.tvNavHome.isSelected = false
        binding.navQibla.isSelected = false
        binding.ivNavQibla.isSelected = false
        binding.tvNavQibla.isSelected = false
        binding.navMore.isSelected = false
        binding.ivNavMore.isSelected = false
        binding.tvNavMore.isSelected = false
    }

    private fun setupNavigation() {
        binding.navHome.setOnClickWithAnimation { finish() }
        binding.navQibla.setOnClickWithAnimation {
            startActivity(Intent(this, QiblaActivity::class.java))
            finish()
        }
        binding.navBook.setOnClickWithAnimation { /* already here */ }
        binding.navMore.setOnClickWithAnimation {
            startActivity(Intent(this, MoreActivity::class.java))
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
