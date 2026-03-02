package com.example.al_aalim

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.al_aalim.adapter.CountryLocationAdapter
import com.example.al_aalim.adapter.RamadanDayAdapter
import com.example.al_aalim.databinding.ActivityRamadanBinding
import com.example.al_aalim.model.CountdownType
import com.example.al_aalim.model.RamadanDay
import com.example.al_aalim.model.RamadanMonth
import com.example.al_aalim.repository.RamadanRepository
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.utils.LanguageManager
import com.example.al_aalim.utils.LocationManager
import kotlinx.coroutines.launch

class RamadanActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityRamadanBinding
    private lateinit var ramadanRepository: RamadanRepository
    private var ramadanMonth: RamadanMonth? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null
    
    companion object {
        private const val TAG = "RamadanActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityRamadanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Initialize repository
        ramadanRepository = RamadanRepository(this)
        
        // Setup UI
        setupHeaderActions()
        setupRecyclerView()
        
        // Load Ramadan data
        loadRamadanData()
    }
    
    private fun setupHeaderActions() {
        binding.ivBackHeader.setOnClickWithAnimation {
            finish()
        }
        
        binding.btnChangeLocation.setOnClickWithAnimation {
            showLocationBottomSheet()
        }
    }
    
    private var selectedLocationIndex = -1
    
    private fun showLocationBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_location, null)
        
        val rvLocations = bottomSheetView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_locations)
        val btnChoose = bottomSheetView.findViewById<androidx.cardview.widget.CardView>(R.id.btn_choose)
        val btnUseMyLocation = bottomSheetView.findViewById<View>(R.id.btn_use_my_location)
        val etSearch = bottomSheetView.findViewById<android.widget.EditText>(R.id.et_search_location)
        val skeletonContainer = bottomSheetView.findViewById<View>(R.id.skeleton_container)
        
        // Show/hide "Use My Location" button based on permission status
        if (LocationManager.hasLocation) {
            btnUseMyLocation.visibility = View.VISIBLE
        } else {
            btnUseMyLocation.visibility = View.GONE
        }
        
        // Reset selection
        selectedLocationIndex = -1
        
        // Setup RecyclerView with empty list initially
        rvLocations.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = com.example.al_aalim.adapter.CountryLocationAdapter(mutableListOf()) { position ->
            selectedLocationIndex = position
        }
        rvLocations.adapter = adapter
        
        // Initially hide list, skeleton and choose button
        rvLocations.visibility = View.GONE
        skeletonContainer.visibility = View.GONE
        btnChoose.visibility = View.GONE
        
        // Search handler with debounce
        var searchJob: android.os.Handler? = android.os.Handler(android.os.Looper.getMainLooper())
        var searchRunnable: Runnable? = null
        
        // Search filtering
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // Cancel previous search
                searchRunnable?.let { searchJob?.removeCallbacks(it) }
                
                val query = s.toString().trim()
                
                if (query.isEmpty()) {
                    // Hide list, skeleton and choose button when search is empty
                    rvLocations.visibility = View.GONE
                    skeletonContainer.visibility = View.GONE
                    btnChoose.visibility = View.GONE
                    adapter.updateLocations(mutableListOf())
                    selectedLocationIndex = -1
                } else {
                    // Show skeleton loading
                    skeletonContainer.visibility = View.VISIBLE
                    rvLocations.visibility = View.GONE
                    btnChoose.visibility = View.GONE
                    
                    // Debounce search by 300ms
                    searchRunnable = Runnable {
                        val results = com.example.al_aalim.data.CountriesData.searchCountries(query)
                        
                        // Hide skeleton, show results and choose button
                        skeletonContainer.visibility = View.GONE
                        rvLocations.visibility = View.VISIBLE
                        btnChoose.visibility = View.VISIBLE
                        
                        adapter.updateLocations(results.toMutableList())
                        selectedLocationIndex = -1
                    }
                    searchJob?.postDelayed(searchRunnable!!, 300)
                }
            }
        })
        
        // Choose button click
        btnChoose.setOnClickListener {
            if (selectedLocationIndex >= 0 && selectedLocationIndex < adapter.locations.size) {
                val selectedLocation = adapter.locations[selectedLocationIndex]
                
                loadRamadanDataForLocation(
                    selectedLocation.latitude,
                    selectedLocation.longitude,
                    selectedLocation.name
                )
                
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(this, "Please select a location", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Use My Location click (only if location is available)
        btnUseMyLocation.setOnClickListener {
            bottomSheetDialog.dismiss()
            if (LocationManager.hasLocation) {
                loadRamadanData() // Reload with current GPS location
                Toast.makeText(this, "Using current location", Toast.LENGTH_SHORT).show()
            }
        }
        
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Configure bottom sheet
        bottomSheetDialog.behavior.apply {
            skipCollapsed = false
            isFitToContents = true
            isHideable = true
        }
        
        // Set window soft input mode
        bottomSheetDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        
        // Focus on search field when shown
        bottomSheetDialog.setOnShowListener {
            etSearch.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(etSearch, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }
        
        bottomSheetDialog.show()
    }
    
    private fun setupRecyclerView() {
        binding.rvRamadanDays.apply {
            layoutManager = GridLayoutManager(this@RamadanActivity, 7) // 7 columns to fit all 30 days
            adapter = RamadanDayAdapter(emptyList()) { day ->
                onDayClicked(day)
            }
        }
    }
    
    private fun loadRamadanData() {
        if (LocationManager.hasLocation) {
            // User has granted location permission - fetch fresh location
            LocationManager.startLocationUpdates(this)
            
            // Wait a moment for location update, then use it
            handler.postDelayed({
                val latitude = LocationManager.userLatitude
                val longitude = LocationManager.userLongitude
                val locationName = "Current Location"
                
                // Update location display
                binding.tvLocation.text = locationName
                
                // Load Ramadan month data with current location
                loadRamadanMonthData(latitude, longitude, locationName)
            }, 500) // Small delay to allow location update
        } else {
            // Try to get from saved preferences
            val prefs = ramadanRepository.getRamadanPreferences()
            val latitude: Double
            val longitude: Double
            val locationName: String
            
            if (prefs.latitude != 0.0 && prefs.longitude != 0.0) {
                latitude = prefs.latitude
                longitude = prefs.longitude
                locationName = prefs.locationName
            } else {
                // Default to a major city (e.g., Mecca)
                latitude = 21.4225
                longitude = 39.8262
                locationName = "Mecca, Saudi Arabia"
            }
            
            // Update location display
            binding.tvLocation.text = locationName
            
            // Load Ramadan month data
            loadRamadanMonthData(latitude, longitude, locationName)
        }
    }
    
    private fun loadRamadanMonthData(latitude: Double, longitude: Double, locationName: String) {
        lifecycleScope.launch {
            try {
                val result = ramadanRepository.getRamadanMonth(
                    year = 2026,
                    latitude = latitude,
                    longitude = longitude,
                    locationName = locationName
                )
                
                result.onSuccess { month ->
                    ramadanMonth = month
                    updateUI(month)
                    startCountdownTimer()
                }.onFailure { error ->
                    Log.e(TAG, "Error loading Ramadan data", error)
                    Toast.makeText(
                        this@RamadanActivity,
                        "Error loading Ramadan calendar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading Ramadan data", e)
            }
        }
    }
    
    private fun updateUI(month: RamadanMonth) {
        // Update RecyclerView with days
        val adapter = RamadanDayAdapter(month.days) { day ->
            onDayClicked(day)
        }
        binding.rvRamadanDays.adapter = adapter
        
        // Update countdown
        updateCountdown()
    }
    
    private fun updateCountdown() {
        ramadanMonth?.let { month ->
            val countdown = ramadanRepository.getCountdown(month)
            countdown?.let {
                if (it.isActive) {
                    binding.tvCountdownLabel.text = when (it.type) {
                        CountdownType.SUHOOR -> getString(R.string.next_suhoor)
                        CountdownType.IFTAR -> getString(R.string.next_iftar)
                    }
                    binding.tvCountdownTime.text = it.timeRemaining
                    binding.tvEventTime.text = "at ${it.eventTime}"
                } else {
                    binding.tvCountdownLabel.text = "Ramadan 2026"
                    binding.tvCountdownTime.text = it.timeRemaining
                    binding.tvEventTime.text = ""
                }
            }
        }
    }
    
    private fun startCountdownTimer() {
        countdownRunnable = object : Runnable {
            override fun run() {
                updateCountdown()
                handler.postDelayed(this, 60000) // Update every minute
            }
        }
        handler.post(countdownRunnable!!)
    }
    
    private fun stopCountdownTimer() {
        countdownRunnable?.let {
            handler.removeCallbacks(it)
        }
    }
    
    private fun onDayClicked(day: RamadanDay) {
        // Show day details in a toast for now
        Toast.makeText(
            this,
            "Day ${day.dayNumber}\nSuhoor: ${day.suhoorTime}\nIftar: ${day.iftarTime}",
            Toast.LENGTH_SHORT
        ).show()
    }
    

    
    private fun loadRamadanDataForLocation(lat: Double, lng: Double, name: String) {
        lifecycleScope.launch {
            try {
                val result = ramadanRepository.getRamadanMonth(2026, lat, lng, name)
                result.onSuccess { month ->
                    ramadanMonth = month
                    binding.tvLocation.text = name
                    updateUI(month)
                    Toast.makeText(this@RamadanActivity, "Location updated to $name", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(this@RamadanActivity, "Error updating location", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RamadanActivity, "Error updating location", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopCountdownTimer()
        LocationManager.stopLocationUpdates() // Stop location updates when leaving
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }
}
