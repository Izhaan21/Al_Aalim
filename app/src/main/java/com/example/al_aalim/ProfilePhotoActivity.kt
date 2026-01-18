package com.example.al_aalim

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.al_aalim.databinding.ActivityProfilePhotoBinding
import com.example.al_aalim.utils.AnimationUtils.setOnClickWithAnimation
import com.example.al_aalim.utils.ProfileManager
import com.example.al_aalim.auth.FirebaseAuthManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfilePhotoActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityProfilePhotoBinding
    private lateinit var windowInsetsController: WindowInsetsControllerCompat
    private lateinit var authManager: FirebaseAuthManager
    
    // Photo picker related
    private var currentPhotoUri: Uri? = null
    
    // Activity result launchers
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cropLauncher: ActivityResultLauncher<android.content.Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Initialize Firebase Auth Manager
        authManager = FirebaseAuthManager(this)
        
        // Initialize View Binding
        binding = ActivityProfilePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Setup activity result launchers
        setupActivityResultLaunchers()
        
        // Configure system bars for theme
        windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        
        // Set transparent system bars
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // For Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        
        // Hide action bar
        supportActionBar?.hide()
        
        // Setup back button with animation
        binding.ivBack.setOnClickWithAnimation {
            finish()
        }
        
        // Setup camera button with animation
        binding.btnCamera.setOnClickWithAnimation {
            checkCameraPermissionAndOpen()
        }
        
        // Setup album button with animation
        binding.btnAlbum.setOnClickWithAnimation {
            checkGalleryPermissionAndOpen()
        }
        
        // Load current profile image
        loadProfileImage()
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
        
        // Crop result launcher
        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == CropPhotoActivity.RESULT_CROPPED) {
                // Reload the profile image after cropping
                loadProfileImage()
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
        // Launch crop activity to adjust the image
        val intent = android.content.Intent(this, CropPhotoActivity::class.java)
        intent.putExtra(CropPhotoActivity.EXTRA_IMAGE_URI, uri)
        cropLauncher.launch(intent)
    }
}
