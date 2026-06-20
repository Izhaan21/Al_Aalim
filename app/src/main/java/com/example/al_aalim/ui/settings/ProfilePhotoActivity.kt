package com.example.al_aalim.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import com.example.al_aalim.R
import com.example.al_aalim.utils.ProfileManager
import com.example.al_aalim.auth.FirebaseAuthManager
import com.example.al_aalim.ui.theme.Gold
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfilePhotoActivity : ComponentActivity() {
    
    private lateinit var accountViewModel: com.example.al_aalim.viewmodel.AccountViewModel
    
    private var currentPhotoUri: Uri? = null
    
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cropLauncher: ActivityResultLauncher<Intent>
    
    private val profileBitmapState = mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val factory = com.example.al_aalim.viewmodel.ViewModelFactory(this)
        accountViewModel = androidx.lifecycle.ViewModelProvider(this, factory)[com.example.al_aalim.viewmodel.AccountViewModel::class.java]
        
        setupActivityResultLaunchers()
        loadProfileImage()

        setContent {
            com.example.al_aalim.ui.theme.AlAalimTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    com.example.al_aalim.ui.theme.BackgroundGradientStart,
                                    com.example.al_aalim.ui.theme.BackgroundGradientEnd
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { finish() }) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x33FFFFFF)).border(1.dp, Color(0x33FFFFFF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(id = R.drawable.ic_back), "Back", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Gold, RoundedCornerShape(16.dp))
                                .background(Color(0xFF233F40))
                                .padding(horizontal = 24.dp, vertical = 6.dp)
                        ) {
                            Text("Profile Photo", color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        Box(modifier = Modifier.size(44.dp))
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val bmp = profileBitmapState.value
                            if (bmp != null) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val initials = accountViewModel.currentUser?.uid?.let { ProfileManager.getUserInitials(this@ProfilePhotoActivity, it) } ?: "IS"
                                Text(initials, color = Color.White, fontSize = 80.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))))
                                .clickable { checkCameraPermissionAndOpen() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_camera), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Take Photo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 40.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))))
                                .clickable { checkGalleryPermissionAndOpen() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.ic_photos), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Choose from Album", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Show delete button only when a profile photo exists
                        if (profileBitmapState.value != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 40.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Brush.linearGradient(listOf(Gold, Color(0xFFC59A45))))
                                    .clickable { deleteProfilePhoto() },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painterResource(R.drawable.ic_account_delete), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text("Delete Profile Photo", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }
    
    private fun setupActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleSelectedImage(it) }
        }
        
        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                currentPhotoUri?.let { handleSelectedImage(it) }
            }
        }
        
        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openCamera() else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
        
        galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) openGallery() else Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show()
        }
        
        cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == CropPhotoActivity.RESULT_CROPPED) {
                loadProfileImage()
            }
        }
    }
    
    private fun loadProfileImage() {
        val userId = accountViewModel.currentUser?.uid ?: return
        profileBitmapState.value = ProfileManager.getProfileImage(this, userId)
    }
    
    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun checkGalleryPermissionAndOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            openGallery()
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            } else {
                galleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
    
    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
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
    
    private fun deleteProfilePhoto() {
        val userId = accountViewModel.currentUser?.uid ?: return
        val deleted = ProfileManager.deleteProfileImage(this, userId)
        if (deleted) {
            profileBitmapState.value = null
            Toast.makeText(this, "Profile photo removed", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Failed to remove photo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        val intent = Intent(this, CropPhotoActivity::class.java)
        intent.putExtra(CropPhotoActivity.EXTRA_IMAGE_URI, uri)
        cropLauncher.launch(intent)
    }
}
