package com.example.al_aalim

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.databinding.ActivityMainBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private var currentPhotoUri: Uri? = null
    
    // Camera permission launcher
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, open camera
            openCamera()
        } else {
            // Permission denied
            android.widget.Toast.makeText(this, "Camera permission is required", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Photo captured successfully
            val photoUri = currentPhotoUri
            photoUri?.let { uri ->
                android.widget.Toast.makeText(this, "Photo captured: $uri", android.widget.Toast.LENGTH_SHORT).show()
                // TODO: Handle the captured photo (upload, display, etc.)
            }
        } else {
            android.widget.Toast.makeText(this, "Photo capture cancelled", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Configure system bars for immersive mode
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // Hide system bars by default, show when user swipes from edge
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Set dark icons for better visibility when bars appear
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
        
        // Enable smooth keyboard animations (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
        
        // Set window soft input mode for smooth keyboard transitions
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
        
        // Hide action bar for cleaner look
        supportActionBar?.hide()
        
        // Handle window insets for the main layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            
        // Hide bottom navigation when keyboard is visible, show when hidden
        val isKeyboardVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
        if (isKeyboardVisible) {
            // Keyboard is visible - hide bottom navigation
            binding.bottomNavigation.visibility = android.view.View.GONE
        } else {
            // Keyboard is hidden - show bottom navigation
            binding.bottomNavigation.visibility = android.view.View.VISIBLE
        }
        windowInsets
        }
        
        // Hide system bars initially
        hideSystemBars()
        
        // Setup bottom navigation
        setupNavigation()
        
        // Set Home as active
        setActiveNavigation()
        
        // Handle keyboard visibility for chat input
        setupKeyboardHandling()
        
        // Setup attachment button click handler
        binding.ivAddAttachment.setOnClickListener {
            showAttachmentOptions()
        }
        
        // Request focus on chat input (keyboard will show naturally when user taps)
        binding.etChatInput.requestFocus()
    }
    
    private fun setupKeyboardHandling() {
        binding.etChatInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Post with delay to ensure keyboard is shown
                binding.chatScroll.postDelayed({
                    // Scroll to bottom smoothly - with null safety check
                    binding.chatScroll.getChildAt(0)?.let { child ->
                        binding.chatScroll.smoothScrollTo(0, child.height)
                    }
                }, 300)
            }
        }
        
        // Also handle when user clicks on the input field
        binding.etChatInput.setOnClickListener {
            binding.chatScroll.postDelayed({
                // Scroll to bottom smoothly - with null safety check
                binding.chatScroll.getChildAt(0)?.let { child ->
                    binding.chatScroll.smoothScrollTo(0, child.height)
                }
            }, 300)
        }
    }
    
    private fun setActiveNavigation() {
        // Set Home as selected
        binding.navHome.isSelected = true
        binding.ivNavHome.isSelected = true
        binding.tvNavHome.isSelected = true
        
        // Ensure others are not selected
        binding.navQibla.isSelected = false
        binding.ivNavQibla.isSelected = false
        binding.tvNavQibla.isSelected = false
        
        binding.navBook.isSelected = false
        binding.ivNavBook.isSelected = false
        binding.tvNavBook.isSelected = false

        binding.navMore.isSelected = false
        binding.ivNavMore.isSelected = false
        binding.tvNavMore.isSelected = false
    }
    
    private fun setupNavigation() {
        // Qibla navigation - using View Binding
        binding.navQibla.setOnClickWithAnimation {
            val intent = Intent(this, QiblaActivity::class.java)
            startActivity(intent)
        }
        
        // Book/Quran navigation
        binding.navBook.setOnClickWithAnimation {
            val intent = Intent(this, QuranActivity::class.java)
            startActivity(intent)
        }

        // More navigation
        binding.navMore.setOnClickWithAnimation {
            val intent = Intent(this, MoreActivity::class.java)
            startActivity(intent)
        }
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-hide system bars when app regains focus
            hideSystemBars()
        }
    }
    
    private fun hideSystemBars() {
        // Hide both status bar and navigation bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
    
    private fun showAttachmentOptions() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_attachment, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        
        // Set up click listeners for each option
        bottomSheetView.findViewById<android.view.View>(R.id.option_camera).setOnClickListener {
            bottomSheetDialog.dismiss()
            checkCameraPermission()
        }
        
        bottomSheetView.findViewById<android.view.View>(R.id.option_photos).setOnClickListener {
            // TODO: Implement photo gallery functionality
            android.widget.Toast.makeText(this, "Photos selected", android.widget.Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }
        
        bottomSheetView.findViewById<android.view.View>(R.id.option_files).setOnClickListener {
            // TODO: Implement file picker functionality
            android.widget.Toast.makeText(this, "Files selected", android.widget.Toast.LENGTH_SHORT).show()
            bottomSheetDialog.dismiss()
        }
        
        
        bottomSheetDialog.show()
    }
    
    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                openCamera()
            }
            else -> {
                // Request permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    
    private fun openCamera() {
        try {
            // Create a temporary file for the photo
            val photoFile = createImageFile()
            
            // Get URI using FileProvider
            currentPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            
            // Launch camera
            cameraLauncher.launch(currentPhotoUri!!)
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Error opening camera: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    
    private fun createImageFile(): File {
        // Create an image file name with timestamp
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }
    
    
    
}