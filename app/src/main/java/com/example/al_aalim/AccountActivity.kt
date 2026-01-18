package com.example.al_aalim

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.auth.FirebaseAuthManager
import com.example.al_aalim.databinding.ActivityAccountBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.utils.ProfileManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAccountBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var authManager: FirebaseAuthManager
    
    // Photo picker related
    private var currentPhotoUri: Uri? = null
    private var bottomSheetProfileImageView: ImageView? = null
    private var bottomSheetInitialsView: TextView? = null
    private var currentBottomSheetDialog: BottomSheetDialog? = null
    
    // Activity result launchers
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth Manager
        authManager = FirebaseAuthManager(this)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize View Binding
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup activity result launchers
        setupActivityResultLaunchers()
        
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
        
        // Setup back button with animation
        binding.ivBack.setOnClickWithAnimation {
            finish()
        }
        
        // Setup edit profile button with animation
        binding.btnEditProfile.setOnClickWithAnimation {
            showEditProfileBottomSheet()
        }
        
        // Setup logout button with animation
        binding.btnLogout.setOnClickWithAnimation {
            performLogout()
        }
        
        // Load and display profile image on activity start
        loadProfileImage()
    }
    
    override fun onResume() {
        super.onResume()
        loadProfileImage()
        loadUserGreeting()
    }
    
    private fun setupActivityResultLaunchers() {
        // Gallery picker launcher
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                handleSelectedImage(it)
            }
        }
        
        // Camera launcher
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let { uri ->
                    handleSelectedImage(uri)
                }
            }
        }
        
        // Camera permission launcher
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Gallery permission launcher (for Android 12 and below)
        galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                openGallery()
            } else {
                Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadProfileImage() {
        val userId = authManager.currentUser?.uid ?: return
        val profileBitmap = ProfileManager.getProfileImage(this, userId)
        if (profileBitmap != null) {
            binding.ivProfilePhoto.setImageBitmap(profileBitmap)
            binding.ivProfilePhoto.visibility = View.VISIBLE
            binding.tvAvatarInitials.visibility = View.GONE
        } else {
            binding.ivProfilePhoto.visibility = View.GONE
            binding.tvAvatarInitials.visibility = View.VISIBLE
            binding.tvAvatarInitials.text = ProfileManager.getUserInitials(this, userId)
        }
    }
    
    private fun loadUserGreeting() {
        val userId = authManager.currentUser?.uid ?: return
        val tvGreeting = findViewById<TextView>(R.id.tv_greeting)
        val userName = ProfileManager.getUserName(this, userId)
        tvGreeting?.text = getString(R.string.account_greeting_format, userName)
    }
    
    private fun showEditProfileBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        currentBottomSheetDialog = bottomSheetDialog
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_edit_profile, null)
        
        // Get references to views
        val etName = bottomSheetView.findViewById<EditText>(R.id.et_name)
        val etUsername = bottomSheetView.findViewById<EditText>(R.id.et_username)
        val btnSave = bottomSheetView.findViewById<CardView>(R.id.btn_save_profile)
        val btnCancel = bottomSheetView.findViewById<TextView>(R.id.btn_cancel)
        val btnChangePhoto = bottomSheetView.findViewById<CardView>(R.id.btn_change_photo)
        val ivProfilePhoto = bottomSheetView.findViewById<ImageView>(R.id.iv_profile_photo)
        val tvInitials = bottomSheetView.findViewById<TextView>(R.id.tv_avatar_initials)
        
        // Store references for photo picker callback
        bottomSheetProfileImageView = ivProfilePhoto
        bottomSheetInitialsView = tvInitials
        
        val userId = authManager.currentUser?.uid ?: return
        
        // Pre-fill with saved values
        etName.setText(ProfileManager.getUserName(this, userId))
        etUsername.setText(ProfileManager.getUsername(this, userId))
        
        // Display current profile image in bottom sheet
        val profileBitmap = ProfileManager.getProfileImage(this, userId)
        if (profileBitmap != null) {
            ivProfilePhoto.setImageBitmap(profileBitmap)
            ivProfilePhoto.visibility = View.VISIBLE
            tvInitials.visibility = View.GONE
        } else {
            ivProfilePhoto.visibility = View.GONE
            tvInitials.visibility = View.VISIBLE
            tvInitials.text = ProfileManager.getUserInitials(this, userId)
        }
        
        // Handle save button click
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val username = etUsername.text.toString().trim()
            
            if (name.isNotEmpty() && username.isNotEmpty()) {
                ProfileManager.saveUserName(this, userId, name)
                ProfileManager.saveUsername(this, userId, username)
                loadProfileImage() // Refresh main activity profile
                Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Handle cancel button click
        btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }
        
        // Handle change photo button click - open full screen profile photo activity
        btnChangePhoto.setOnClickListener {
            bottomSheetDialog.dismiss()
            val intent = android.content.Intent(this, ProfilePhotoActivity::class.java)
            startActivity(intent)
        }
        
        bottomSheetDialog.setOnDismissListener {
            bottomSheetProfileImageView = null
            bottomSheetInitialsView = null
            currentBottomSheetDialog = null
        }
        
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }
    
    private fun showPhotoPickerOptions() {
        val photoPickerDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        val photoPickerView = layoutInflater.inflate(R.layout.bottom_sheet_attachment, null)
        
        // Get option views
        val optionCamera = photoPickerView.findViewById<View>(R.id.option_camera)
        val optionPhotos = photoPickerView.findViewById<View>(R.id.option_photos)
        val optionFiles = photoPickerView.findViewById<View>(R.id.option_files)
        
        // Hide files option - not needed for profile photo
        optionFiles.visibility = View.GONE
        
        // Camera option
        optionCamera.setOnClickListener {
            photoPickerDialog.dismiss()
            checkCameraPermissionAndOpen()
        }
        
        // Gallery option
        optionPhotos.setOnClickListener {
            photoPickerDialog.dismiss()
            checkGalleryPermissionAndOpen()
        }
        
        photoPickerDialog.setContentView(photoPickerView)
        photoPickerDialog.show()
    }
    
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun checkGalleryPermissionAndOpen() {
        // For Android 13+, we don't need READ_EXTERNAL_STORAGE for picking images
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openGallery()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10-12, scoped storage handles this
            openGallery()
        } else {
            // For older versions, check permission
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED -> {
                    openGallery()
                }
                else -> {
                    galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }
    
    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            currentPhotoUri = photoUri
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error opening camera", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PROFILE_${timeStamp}_", ".jpg", storageDir)
    }
    
    private fun handleSelectedImage(uri: Uri) {
        val userId = authManager.currentUser?.uid ?: return
        
        // Save the image using ProfileManager
        val saved = ProfileManager.saveProfileImage(this, userId, uri)
        
        if (saved) {
            // Update the bottom sheet image if it's still visible
            val profileBitmap = ProfileManager.getProfileImage(this, userId)
            if (profileBitmap != null) {
                bottomSheetProfileImageView?.setImageBitmap(profileBitmap)
                bottomSheetProfileImageView?.visibility = View.VISIBLE
                bottomSheetInitialsView?.visibility = View.GONE
            }
            
            // Update the main activity profile image
            loadProfileImage()
            
            Toast.makeText(this, "Profile photo updated!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to save profile photo", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun performLogout() {
        // Clear current user session (profile data persists for next login)
        ProfileManager.clearCurrentUser(this)
        
        // Sign out from Firebase
        authManager.signOut()
        
        // Navigate to LoginActivity and clear back stack
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        finish()
    }
}

